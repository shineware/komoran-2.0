/*
 * KOMORAN 2.0 - Korean Morphology Analyzer
 *
 * Copyright 2014 Shineware http://www.shineware.co.kr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.shineware.nlp.komoran.core.lattice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import kr.co.shineware.nlp.komoran.constant.SCORE;
import kr.co.shineware.nlp.komoran.constant.SYMBOL;
import kr.co.shineware.nlp.komoran.core.lattice.model.LatticeNode;
import kr.co.shineware.nlp.komoran.exception.FileFormatException;
import kr.co.shineware.nlp.komoran.modeler.model.PosTable;
import kr.co.shineware.nlp.komoran.modeler.model.Transition;
import kr.co.shineware.nlp.komoran.parser.KoreanUnitParser;
import kr.co.shineware.util.common.model.Pair;

public class Lattice {
	//key = endIdx
	//value.key = prev lattice node's hashcode
	//value.value = prev lattice node that matched hashcode
	private Map<Integer,Map<Integer,LatticeNode>> lattice;
	private Transition transition;
	private PosTable table;
	private KoreanUnitParser unitParser;
	private int totalIdx = -1;
	private int prevStartIdx = 0;
	private int nbest = 1;
	private static final String ANSWER_SPLITER = " ";
	private static final String WORD_POS_SPLITER = "\\/";

	public Lattice(PosTable table){
		this.table = table;
		this.init();
	}
	public void init() {


		this.lattice = null;
		this.lattice = new HashMap<Integer, Map<Integer,LatticeNode>>();
		Map<Integer,LatticeNode> initNodeMap = new HashMap<>();
		LatticeNode initNode = new LatticeNode();
		initNode.setMorph(SYMBOL.START);
		initNode.setPosId(this.table.getId(SYMBOL.START));
		initNode.setScore(0);
		initNodeMap.put(initNode.hashCode(), initNode);
		this.lattice.put(0, initNodeMap);
		this.unitParser = new KoreanUnitParser();

		prevStartIdx = 0;

		//init
		initNode = null;
	}

	public void put(int beginIdx,int endIdx, List<Pair<Integer, Double>> posIdScoreList, String morph){
		for (Pair<Integer, Double> pair : posIdScoreList) {
			this.put(beginIdx, endIdx, morph, pair.getFirst(), pair.getSecond());
		}
	}

	public void put(int beginIdx,int endIdx,String morph,int posId,double score){
		this.put(beginIdx, endIdx, morph, posId, posId, score);
	}

	public void put(int beginIdx,int endIdx,String morph,int firstPosId,int lastPosId, double score){
		Map<Integer,LatticeNode> prevNode = this.lattice.get(beginIdx);
		if(prevNode == null){
			;
		}else{
			//for nbest
			if(this.nbest > 1){
				List<LatticeNode> nBestPrevNodeList = this.getNBestPrevNodes(prevNode, morph, firstPosId);
				for (LatticeNode maxPrevNode : nBestPrevNodeList) {
							
					if(maxPrevNode == null){
						;
					}else{
						Map<Integer,LatticeNode> curNodeMap = this.lattice.get(endIdx);
						if(curNodeMap == null){
							curNodeMap = new HashMap<>();
						}
						LatticeNode curNode = new LatticeNode();
						curNode.setMorph(morph);
						curNode.setPosId(lastPosId);
						double curNodeScore = maxPrevNode.getScore()+this.transition.get(maxPrevNode.getPosId(), firstPosId)+score;
						curNode.setScore(curNodeScore);
						curNode.setPrevIdx(beginIdx);
						curNode.setPrevHashcode(maxPrevNode.hashCode());
						curNodeMap.put(curNode.hashCode(), curNode);
						lattice.put(endIdx, curNodeMap);
						this.totalIdx = Math.max(this.totalIdx, endIdx);

						curNode = null;
						curNodeMap = null;
					}
					prevNode = null;
					maxPrevNode = null;
				}
			}
			//for 1-best
			else{
				LatticeNode maxPrevNode = this.getMaxPrevNode(prevNode,morph,firstPosId);			
				if(maxPrevNode == null){
					;
				}else{
					Map<Integer,LatticeNode> curNodeMap = this.lattice.get(endIdx);
					if(curNodeMap == null){
						curNodeMap = new HashMap<>();
					}
					LatticeNode curNode = new LatticeNode();
					curNode.setMorph(morph);
					curNode.setPosId(lastPosId);
					double curNodeScore = maxPrevNode.getScore()+this.transition.get(maxPrevNode.getPosId(), firstPosId)+score;
					curNode.setScore(curNodeScore);
					curNode.setPrevIdx(beginIdx);
					curNode.setPrevHashcode(maxPrevNode.hashCode());
					curNodeMap.put(curNode.hashCode(), curNode);
					lattice.put(endIdx, curNodeMap);
					this.totalIdx = Math.max(this.totalIdx, endIdx);

					curNode = null;
					curNodeMap = null;
				}
				prevNode = null;
				maxPrevNode = null;
			}
		}
	}
	private List<LatticeNode> getNBestPrevNodes(Map<Integer, LatticeNode> prevNode,
			String morph, int posId){
		List<LatticeNode> nbestPrevNodeList = new ArrayList<LatticeNode>(this.nbest);
		
		Set<Entry<Integer,LatticeNode>> prevNodeSet = prevNode.entrySet();
		//		double minScore = Double.NEGATIVE_INFINITY;
		//		LatticeNode maxPrevNode = null;
		for (Entry<Integer, LatticeNode> prevNodeEntry : prevNodeSet) {
			double prevNodeScore = prevNodeEntry.getValue().getScore();
			int prevNodePosId = prevNodeEntry.getValue().getPosId();

			Double transitionScore = this.transition.get(prevNodePosId, posId);

			if(transitionScore == null){
				continue;
			}

			//자소 결합규칙 체크
			if(posId == this.table.getId(SYMBOL.JKO)){
				if(this.hasJongsung(prevNodeEntry.getValue().getMorph())){
					if(morph.charAt(0) != 'ㅇ'){
						continue;
					}
				}else{
					if(morph.charAt(0) == 'ㅇ'){
						continue;
					}
				}
			}else if(posId == this.table.getId(SYMBOL.JKS)
					|| posId == this.table.getId(SYMBOL.JKC)){
				if(this.hasJongsung(prevNodeEntry.getValue().getMorph())){
					if(morph.equals("ㄱㅏ")){
						continue;
					}
				}else{
					if(morph.equals("ㅇㅣ")){
						continue;
					}
				}
			}

			if(nbestPrevNodeList.size() < nbest){				
				nbestPrevNodeList.add(prevNodeEntry.getValue());
				continue;
			}


			int minIdx = 0;
			double minNbestScore = nbestPrevNodeList.get(0).getScore();
			for(int i=1;i<nbestPrevNodeList.size();i++){
				if(minNbestScore > nbestPrevNodeList.get(i).getScore()){
					minIdx = i;
					minNbestScore = nbestPrevNodeList.get(i).getScore();
				}
			}
			if(prevNodeScore + transitionScore >  minNbestScore){
				nbestPrevNodeList.set(minIdx, prevNodeEntry.getValue());
			}


			transitionScore = null;
		}
		prevNodeSet = null;
		return nbestPrevNodeList;
	}

	private LatticeNode getMaxPrevNode(Map<Integer, LatticeNode> prevNode,
			String morph, int posId) {
		Set<Entry<Integer,LatticeNode>> prevNodeSet = prevNode.entrySet();
		double maxScore = Double.NEGATIVE_INFINITY;
		LatticeNode maxPrevNode = null;
		for (Entry<Integer, LatticeNode> prevNodeEntry : prevNodeSet) {
			double prevNodeScore = prevNodeEntry.getValue().getScore();
			int prevNodePosId = prevNodeEntry.getValue().getPosId();

			Double transitionScore = this.transition.get(prevNodePosId, posId);

			if(transitionScore == null){
				continue;
			}

			//자소 결합규칙 체크
			if(posId == this.table.getId(SYMBOL.JKO)){
				if(this.hasJongsung(prevNodeEntry.getValue().getMorph())){
					if(morph.charAt(0) != 'ㅇ'){
						continue;
					}
				}else{
					if(morph.charAt(0) == 'ㅇ'){
						continue;
					}
				}
			}else if(posId == this.table.getId(SYMBOL.JKS)
					|| posId == this.table.getId(SYMBOL.JKC)){
				if(this.hasJongsung(prevNodeEntry.getValue().getMorph())){
					if(morph.equals("ㄱㅏ")){
						continue;
					}
				}else{
					if(morph.equals("ㅇㅣ")){
						continue;
					}
				}
			}


			if(prevNodeScore + transitionScore > maxScore){
				maxScore = prevNodeScore + transitionScore;
				maxPrevNode = prevNodeEntry.getValue();
			}
			transitionScore = null;
		}
		prevNodeSet = null;
		return maxPrevNode;
	}


	private boolean hasJongsung(String str) {
		char prevLastJaso = str.charAt(str.length()-1);
		if(0x3131 <= prevLastJaso && prevLastJaso <= 0x314e){
			if(prevLastJaso == 0x3138 || prevLastJaso ==  0x3143 || prevLastJaso == 0x3149){
				return false;
			}else{
				return true;
			}
		}
		return false;
	}
	public void setTransition(Transition transition) {
		this.transition = transition;		
	}

	public List<List<List<Pair<String, String>>>> getNbest(String in) {
		Map<Integer,LatticeNode> lastNodes = this.lattice.get(totalIdx);
		if(lastNodes == null){
			Map<Integer,LatticeNode> notAnalyzedNodeMap = new HashMap<>();
			Set<Integer> prevHashSet = lattice.get(this.getPrevStartIdx()).keySet();
			for (Integer prevHashcode : prevHashSet) {
				LatticeNode notAnalyzedNode = new LatticeNode();//NA
				notAnalyzedNode.setMorph(in.substring(this.getPrevStartIdx(),totalIdx));
				notAnalyzedNode.setPosId(this.table.getId(SYMBOL.NA));
				notAnalyzedNode.setPrevHashcode(prevHashcode);
				notAnalyzedNode.setPrevIdx(this.getPrevStartIdx());			
				notAnalyzedNode.setScore(SCORE.NA);
				notAnalyzedNodeMap.put(notAnalyzedNode.hashCode(), notAnalyzedNode);
				notAnalyzedNode = null;
			}
			lattice.put(totalIdx, notAnalyzedNodeMap);
			lastNodes = notAnalyzedNodeMap;
			notAnalyzedNodeMap = null;
			prevHashSet = null;
		}

		this.updateEndTransition(lastNodes,in);

		lastNodes = null;
		if(this.lattice.get(totalIdx+1) == null){
			return null;
		}
		//		this.print(totalIdx+2);
		return this.backTrackingNbest();
	}

	public List<List<Pair<String, String>>> getMax(String in) {
		Map<Integer,LatticeNode> lastNodes = this.lattice.get(totalIdx);
		if(lastNodes == null){
			Map<Integer,LatticeNode> notAnalyzedNodeMap = new HashMap<>();
			Set<Integer> prevHashSet = lattice.get(this.getPrevStartIdx()).keySet();
			for (Integer prevHashcode : prevHashSet) {
				LatticeNode notAnalyzedNode = new LatticeNode();//NA
				notAnalyzedNode.setMorph(in.substring(this.getPrevStartIdx(),totalIdx));
				notAnalyzedNode.setPosId(this.table.getId(SYMBOL.NA));
				notAnalyzedNode.setPrevHashcode(prevHashcode);
				notAnalyzedNode.setPrevIdx(this.getPrevStartIdx());			
				notAnalyzedNode.setScore(SCORE.NA);
				notAnalyzedNodeMap.put(notAnalyzedNode.hashCode(), notAnalyzedNode);
				notAnalyzedNode = null;
			}
			lattice.put(totalIdx, notAnalyzedNodeMap);
			lastNodes = notAnalyzedNodeMap;
			notAnalyzedNodeMap = null;
			prevHashSet = null;
		}

		this.updateEndTransition(lastNodes,in);

		lastNodes = null;
		if(this.lattice.get(totalIdx+1) == null){
			return null;
		}
		//		this.print(totalIdx+2);
		return this.backTracking();
	}	

	private List<List<List<Pair<String, String>>>> backTrackingNbest(){
		List<List<List<Pair<String, String>>>> nbestList = new ArrayList<List<List<Pair<String,String>>>>();
		Set<LatticeNode> trackNodes = new HashSet<LatticeNode>();
		for(int j=0;j<nbest;j++){
			double maxScore = Double.NEGATIVE_INFINITY;
			LatticeNode maxNode = null;
			Map<Integer,LatticeNode> endNodeMap = this.lattice.get(totalIdx+1);
			Set<Entry<Integer,LatticeNode>> endNodeSet = endNodeMap.entrySet();
			for (Entry<Integer, LatticeNode> endNodeEntry : endNodeSet) {
				LatticeNode endNode = endNodeEntry.getValue();
				if(trackNodes.contains(endNode))continue;
				if(maxScore < endNode.getScore()){
					maxScore = endNode.getScore();
					maxNode = endNode;
				}
				endNode = null;
			}

			endNodeMap = null;
			endNodeSet = null;

			if(maxNode != null){			
				trackNodes.add(maxNode);
				List<LatticeNode> maxNodeList = new ArrayList<>();
				LatticeNode traceNode = maxNode;
				while(true){
					traceNode = this.lattice.get(traceNode.getPrevIdx()).get(traceNode.getPrevHashcode());
					maxNodeList.add(traceNode);
					if(traceNode.getPrevIdx() == 0)break;
				}
				List<List<Pair<String,String>>> sentenceResult = new ArrayList<>();
				List<Pair<String,String>> wordResult = new ArrayList<>();
				for(int i=maxNodeList.size()-1;i>=0;i--){
					if(maxNodeList.get(i).getMorph().trim().equals(SYMBOL.SPACE)){
						if(wordResult.size() != 0){
							sentenceResult.add(wordResult);
						}
						wordResult = null;
						wordResult = new ArrayList<>();
					}else{
						String latticeNode = maxNodeList.get(i).getMorph()+"/"+this.table.getPos(maxNodeList.get(i).getPosId());
						try {
							this.parseAnswer(latticeNode, wordResult);
						} catch (FileFormatException e) {
							e.printStackTrace();
						}
						latticeNode = null;
					}				
				}

				if(wordResult.size() != 0){
					sentenceResult.add(wordResult);
				}

				maxNodeList = null;
				traceNode = null;
				wordResult = null;
				//			System.out.println(maxScore);
				//				return sentenceResult;
				nbestList.add(sentenceResult);
			}else{
				continue;
			}
		}
		return nbestList;
	}

	private List<List<Pair<String, String>>> backTracking() {
		LatticeNode maxNode = null;
		double maxScore = Double.NEGATIVE_INFINITY;
		Map<Integer,LatticeNode> endNodeMap = this.lattice.get(totalIdx+1);
		Set<Entry<Integer,LatticeNode>> endNodeSet = endNodeMap.entrySet();
		for (Entry<Integer, LatticeNode> endNodeEntry : endNodeSet) {
			LatticeNode endNode = endNodeEntry.getValue();
			if(maxScore < endNode.getScore()){
				maxScore = endNode.getScore();
				maxNode = endNode;
			}
			endNode = null;
		}

		endNodeMap = null;
		endNodeSet = null;

		if(maxNode != null){			

			List<LatticeNode> maxNodeList = new ArrayList<>();
			LatticeNode traceNode = maxNode;
			while(true){
				traceNode = this.lattice.get(traceNode.getPrevIdx()).get(traceNode.getPrevHashcode());
				maxNodeList.add(traceNode);
				if(traceNode.getPrevIdx() == 0)break;
			}
			List<List<Pair<String,String>>> sentenceResult = new ArrayList<>();
			List<Pair<String,String>> wordResult = new ArrayList<>();
			for(int i=maxNodeList.size()-1;i>=0;i--){
				if(maxNodeList.get(i).getMorph().trim().equals(SYMBOL.SPACE)){
					if(wordResult.size() != 0){
						sentenceResult.add(wordResult);
					}
					wordResult = null;
					wordResult = new ArrayList<>();
				}else{
					String latticeNode = maxNodeList.get(i).getMorph()+"/"+this.table.getPos(maxNodeList.get(i).getPosId());
					try {
						this.parseAnswer(latticeNode, wordResult);
					} catch (FileFormatException e) {
						e.printStackTrace();
					}
					latticeNode = null;
				}				
			}

			if(wordResult.size() != 0){
				sentenceResult.add(wordResult);
			}

			maxNodeList = null;
			traceNode = null;
			wordResult = null;
			//			System.out.println(maxScore);
			return sentenceResult;
		}else{
			return null;
		}
	}
	private void updateEndTransition(Map<Integer,LatticeNode> lastNodeMap, String in) {

		Map<Integer,LatticeNode> endNodeMap = new HashMap<>();

		Set<Entry<Integer,LatticeNode>> lastNodeSet = lastNodeMap.entrySet();
		boolean isConnectedEndNode = false;
		for (Entry<Integer, LatticeNode> lastNodeEntry : lastNodeSet) {
			LatticeNode lastNode = lastNodeEntry.getValue();
			double lastNodeScore = lastNode.getScore();
			int lastNodePosId = lastNode.getPosId();

			Double transitionScore = this.transition.get(lastNodePosId, this.table.getId(SYMBOL.END));
			if(transitionScore == null){
				lastNode = null;
				continue;
			}

			if(lastNodePosId == this.table.getId(SYMBOL.NA)){
				transitionScore = SCORE.NA;
			}
			isConnectedEndNode = true;
			LatticeNode endNode = new LatticeNode();
			endNode.setMorph(SYMBOL.END);
			endNode.setPosId(this.table.getId(SYMBOL.END));
			endNode.setPrevIdx(totalIdx);
			endNode.setPrevHashcode(lastNode.hashCode());
			endNode.setScore(lastNodeScore+transitionScore);
			endNodeMap.put(endNode.hashCode(), endNode);
			lattice.put(totalIdx+1, endNodeMap);

			lastNode = null;
			endNode= null;
			transitionScore = null;
		}
		if(!isConnectedEndNode){

			Map<Integer,LatticeNode> naNodeMap = new HashMap<>();
			LatticeNode naNode = new LatticeNode();
			naNode.setMorph(in.substring(this.prevStartIdx, this.totalIdx));
			naNode.setPosId(this.table.getId(SYMBOL.NA));
			naNode.setPrevHashcode(lattice.get(this.prevStartIdx).keySet().iterator().next());
			naNode.setPrevIdx(this.prevStartIdx);
			naNode.setScore(SCORE.NA);
			naNodeMap.put(naNode.hashCode(), naNode);
			lattice.put(totalIdx, naNodeMap);

			LatticeNode endNode = new LatticeNode();
			endNode.setMorph(SYMBOL.END);
			endNode.setPosId(this.table.getId(SYMBOL.END));
			endNode.setPrevIdx(totalIdx);
			endNode.setPrevHashcode(naNode.hashCode());
			endNode.setScore(0.0);
			endNodeMap.put(endNode.hashCode(), endNode);
			lattice.put(totalIdx+1, endNodeMap);

			naNodeMap = null;
			naNode = null;
			endNode = null;
		}

		lastNodeSet = null;
		endNodeMap = null;
	}
	private void parseAnswer(String answer,List<Pair<String,String>> answerList) throws FileFormatException {
		String[] tmp = answer.trim().split(ANSWER_SPLITER);

		String prevWord = "";
		for(int i=0;i<tmp.length;i++){

			String token = tmp[i];
			String pos,word;
			String[] wordPos = token.split(WORD_POS_SPLITER);

			if(wordPos.length == 2){
				word = wordPos[0];
				pos = wordPos[1];
			}else if(wordPos.length > 2){
				pos = wordPos[wordPos.length-1];
				word = token.substring(0, token.length()-pos.length()-1);
			}else if(wordPos.length == 1){
				prevWord += token+" ";
				continue;
			}else{
				word = "";
				pos = "";
			}

			if(word.trim().length() == 0 || pos.trim().length() == 0){
				//				throw new FileFormatException("Lattice Parse Answer Format Error. "+answer);
			}else{
				word = prevWord + word;
				answerList.add(new Pair<String, String>(this.unitParser.combine(word), pos));
			}
			prevWord = "";

			token = null;
			pos = null;
			word = null;
			wordPos = null;
		}
		tmp = null;
		prevWord = null;
	}
	public void setEndIdx(int endIdx) {
		this.totalIdx = endIdx;		
	}
	public void bridgingSpace(String in, int i) {
		Map<Integer,LatticeNode> lastNodeMap = this.lattice.get(i);
		if(lastNodeMap == null){
			Map<Integer,LatticeNode> notAnalyzedNodeMap = new HashMap<>();
			Map<Integer,LatticeNode> bridgeNodeMap = new HashMap<>();
			Set<Integer> prevHashSet = lattice.get(this.getPrevStartIdx()).keySet();
			for (Integer prevHashcode : prevHashSet) {
				LatticeNode notAnalyzedNode = new LatticeNode();//NA
				notAnalyzedNode.setMorph(in.substring(this.getPrevStartIdx(),i));
				notAnalyzedNode.setPosId(this.table.getId(SYMBOL.NA));
				notAnalyzedNode.setPrevHashcode(prevHashcode);
				notAnalyzedNode.setPrevIdx(this.getPrevStartIdx());			
				notAnalyzedNode.setScore(SCORE.NA);
				notAnalyzedNodeMap.put(notAnalyzedNode.hashCode(), notAnalyzedNode);

				LatticeNode bridgeNode = new LatticeNode(); //SPACE
				bridgeNode.setMorph(SYMBOL.SPACE);
				bridgeNode.setPosId(this.table.getId(SYMBOL.START));
				bridgeNode.setPrevIdx(i);
				bridgeNode.setPrevHashcode(notAnalyzedNode.hashCode());
				bridgeNode.setScore(0.0);
				bridgeNodeMap.put(bridgeNode.hashCode(), bridgeNode);
			}
			lattice.put(i, notAnalyzedNodeMap);
			lattice.put(i+1, bridgeNodeMap);
		}else{
			Map<Integer,LatticeNode> bridgeNodeMap = new HashMap<>();
			Set<Entry<Integer,LatticeNode>> lastNodeSet = lastNodeMap.entrySet();

			double maxScore = Double.NEGATIVE_INFINITY;
			LatticeNode maxPrevNode = null;
			boolean isContinousSpace = false;
			int prevSpaceHashcode = 0;
			for (Entry<Integer, LatticeNode> lastNodeEntry : lastNodeSet) {				
				LatticeNode lastNode = lastNodeEntry.getValue();
				double lastNodeScore = lastNode.getScore();
				int lastNodePosId = lastNode.getPosId();
				if(lastNodePosId == this.table.getId(SYMBOL.START)){
					isContinousSpace = true;
					prevSpaceHashcode = lastNode.hashCode();
					break;
				}
				Double transitionScore = this.transition.get(lastNodePosId, this.table.getId(SYMBOL.END));
				if(transitionScore == null){
					continue;
				}
				if(lastNodeScore+transitionScore > maxScore){
					maxScore = lastNodeScore+transitionScore;
					maxPrevNode = lastNode;
				}
			}
			if(maxPrevNode != null){
				LatticeNode bridgeNode = new LatticeNode();
				bridgeNode.setMorph(SYMBOL.SPACE);
				bridgeNode.setPosId(this.table.getId(SYMBOL.START));
				bridgeNode.setPrevIdx(i);
				bridgeNode.setPrevHashcode(maxPrevNode.hashCode());
				bridgeNode.setScore(maxScore);
				bridgeNodeMap.put(bridgeNode.hashCode(), bridgeNode);
				lattice.put(i+1, bridgeNodeMap);
			}
			else if(isContinousSpace){
				LatticeNode bridgeNode = new LatticeNode();
				bridgeNode.setMorph(SYMBOL.SPACE);
				bridgeNode.setPosId(this.table.getId(SYMBOL.START));
				bridgeNode.setPrevIdx(i);
				bridgeNode.setPrevHashcode(prevSpaceHashcode);
				bridgeNode.setScore(0.0);
				bridgeNodeMap.put(bridgeNode.hashCode(), bridgeNode);
				lattice.put(i+1, bridgeNodeMap);
			}
			else{
				Map<Integer,LatticeNode> naNodeMap = new HashMap<>();
				LatticeNode naNode = new LatticeNode();
				naNode.setMorph(in.substring(this.prevStartIdx, i));
				naNode.setPosId(this.table.getId(SYMBOL.NA));
				naNode.setPrevHashcode(lattice.get(this.prevStartIdx).keySet().iterator().next());
				naNode.setPrevIdx(this.prevStartIdx);
				naNode.setScore(SCORE.NA);
				naNodeMap.put(naNode.hashCode(), naNode);
				lattice.put(i, naNodeMap);

				LatticeNode bridgeNode = new LatticeNode();
				bridgeNode.setMorph(SYMBOL.SPACE);
				bridgeNode.setPosId(this.table.getId(SYMBOL.START));
				bridgeNode.setPrevIdx(i);
				bridgeNode.setPrevHashcode(naNode.hashCode());
				bridgeNode.setScore(0.0);
				bridgeNodeMap.put(bridgeNode.hashCode(), bridgeNode);
				lattice.put(i+1, bridgeNodeMap);
			}
		}
	}
	public int getPrevStartIdx() {
		return prevStartIdx;
	}
	public void setPrevStartIdx(int prevStartIdx) {
		this.prevStartIdx = prevStartIdx;
	}
	public void print() {
		for(int i=0;i<this.totalIdx+1;i++){
			System.out.println("["+i+"]");
			Map<Integer,LatticeNode> singleLattice = this.lattice.get(i);
			if(singleLattice != null){
				for (Integer hashcode : singleLattice.keySet()) {
					System.out.println(hashcode+"="+singleLattice.get(hashcode));
				}
			}else{
				System.out.println("null");
			}
			System.out.println();
		}
	}
	public void print(int idx) {
		for(int i=0;i<idx;i++){
			System.out.println("["+i+"]");
			System.out.println(this.lattice.get(i));
			System.out.println();
		}
	}
	
	/**
	 * 최종 후보 중 가장 큰 값을 갖는 n-best 후보 설정
	 * @param nbest
	 */
	public void setNbest(int nbest) {
		this.nbest = nbest;
	}
}
