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
package kr.co.shineware.nlp.komoran.core.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.co.shineware.ds.trie.TrieDictionary;
import kr.co.shineware.ds.trie.model.TrieNode;
import kr.co.shineware.nlp.komoran.constant.FILENAME;
import kr.co.shineware.nlp.komoran.constant.SCORE;
import kr.co.shineware.nlp.komoran.constant.SYMBOL;
import kr.co.shineware.nlp.komoran.core.analyzer.model.PrevNodes;
import kr.co.shineware.nlp.komoran.core.lattice.Lattice;
import kr.co.shineware.nlp.komoran.corpus.parser.CorpusParser;
import kr.co.shineware.nlp.komoran.corpus.parser.model.ProblemAnswerPair;
import kr.co.shineware.nlp.komoran.interfaces.UnitParser;
import kr.co.shineware.nlp.komoran.modeler.model.IrregularNode;
import kr.co.shineware.nlp.komoran.modeler.model.IrregularTrie;
import kr.co.shineware.nlp.komoran.modeler.model.Observation;
import kr.co.shineware.nlp.komoran.modeler.model.PosTable;
import kr.co.shineware.nlp.komoran.modeler.model.Transition;
import kr.co.shineware.nlp.komoran.parser.KoreanUnitParser;
import kr.co.shineware.util.common.model.Pair;
import kr.co.shineware.util.common.string.StringUtil;

/**
 * 형태소 분석 core에 해당<br>
 * Observation, Transition, IrregularTrie, PosTable, UnitParser, Lattice, FWD을 사용<br>
 * @author Junsoo Shin
 * @version 2.1
 * @since 2.0
 *
 */
public class Komoran {

	private Observation observation;
	private IrregularTrie irrTrie;
	private Transition transition;
	private PosTable table;
	private PrevNodes<TrieNode<List<Pair<Integer,Double>>>> prevNodesRegular;
	private PrevNodes<TrieNode<List<IrregularNode>>> prevNodesIrregular;
	private List<Pair<Integer,IrregularNode>> prevNodesExpand; 
	private UnitParser unitParser;
	private Lattice lattice;
	
	//for ruleParser
	private String ruleMorph;
	private String rulePos;
	private int ruleBeginIdx;
	
	private HashMap<String, List<Pair<String, Integer>>> fwd;	

	/**
	 * 코모란의 생성자로써 객체 생성 시 불규칙,관측,전이 확률 모델 및 품사 테이블이 포함된 경로를 지정해주어야 함<br>
	 * 각 모델 및 테이블의 파일명은 아래와 같은 <br>
	 * - 불규칙 모델 : irregular.model <br>
	 * - 관측 확률 모델 : observation.model <br>
	 * - 전이 확률 모델 : transition.model <br>
	 * - 품사 테이블 : pos.tabel <br>
	 * @param path 형태소 분석에 필요한 데이터 파일들의 경로
	 */
	public Komoran(String path){
		this.init();
		this.load(path);
	}

	/**
	 * 각종 리소스 초기화
	 */
	private void init(){
		this.table = null;
		this.observation = null;
		this.transition = null;
		this.unitParser = null;
		this.irrTrie = null;

		this.table = new PosTable();
		this.observation = new Observation();
		this.transition = new Transition();
		this.unitParser = new KoreanUnitParser();
		this.irrTrie = new IrregularTrie();
	}
	
	/**
	 * 형태소 분석에 사용되는 실제 데이터를 로딩 <br>
	 * 파일명은 {@link FILENAME}에 기술되어 있음
	 * @param path
	 */
	private void load(String path){
		this.table.load(path+File.separator+FILENAME.POS_TABLE);
		this.observation.load(path+File.separator+FILENAME.OBSERVATION);
		this.transition.load(path+File.separator+FILENAME.TRANSITION);
		this.irrTrie.load(path+File.separator+FILENAME.IRREGULAR_MODEL);
	}
	
	/**
	 * 입력된 텍스트 src를 공백 단위로 잘라 형태소 분석(기존 형태소 분석기와 동일 방법)
	 * @param src
	 * @return
	 */
	public List<List<Pair<String, String>>> analyzeWithoutSpace(String src){
		List<List<Pair<String,String>>> result = new ArrayList<List<Pair<String,String>>>();
		String[] tokens = src.split("[ ]+");
		for (String token : tokens) {
			result.addAll(this.analyze(token));
		}
		return result;
	}
	
	public List<List<List<Pair<String, String>>>> analyzeWithoutSpace(String src,int nbest){
		
		//word<rank<analyze<morph,pos>>>
		List<List<List<Pair<String,String>>>> result = new ArrayList<List<List<Pair<String,String>>>>();
		String[] tokens = src.split("[ ]+");
		for (String token : tokens) {
			//rank<words<analyze<morph,pos>>>
			List<List<List<Pair<String, String>>>> tokenResultList = this.analyze(token,nbest);
			//rank<analyze<morph,pos>>
			List<List<Pair<String,String>>> rankTokenResultList = new ArrayList<>();
			for (List<List<Pair<String, String>>> list : tokenResultList) {
				rankTokenResultList.add(list.get(0));
			}
			result.add(rankTokenResultList);
//			result.addAll(this.analyze(token,nbest));
		}
		return result;
	}
	
	public List<List<List<Pair<String, String>>>> analyze(String src,int nbest){
		if(nbest < 1){
			return new ArrayList<List<List<Pair<String, String>>>>();
		}
		if(src.trim().length() == 0){
			return new ArrayList<List<List<Pair<String, String>>>>();
		}
		//형태소 분석 및 품사 태거를 위한 객체 초기화
		this.lattice = null;
		this.lattice = new Lattice(this.table);
		this.lattice.setTransition(this.transition);
		this.lattice.setNbest(nbest);

		this.prevNodesRegular = null;
		this.prevNodesRegular = new PrevNodes<TrieNode<List<Pair<Integer, Double>>>>();

		this.prevNodesIrregular = null;
		this.prevNodesIrregular = new PrevNodes<TrieNode<List<IrregularNode>>>();

		this.prevNodesExpand = null;
		this.prevNodesExpand = new ArrayList<>();
		
		this.ruleMorph = "";
		this.rulePos = "";
		this.ruleBeginIdx = 0;		
		
		//자소 단위로 분리
		String in = unitParser.parse(src.trim());
		
		for(int i=0;i<in.length();i++){
			//기분석 사전 매칭
			int skipIdx = this.lookupFwd(in,i);
			//매칭된 인덱스 만큼 증가
			if(skipIdx != -1){
				i = skipIdx-1;
				continue;
			}
			
			//규칙 기반의 연속된 숫자, 영어, 한자, 외래어 파싱
			this.ruleParsing(in,i);
			//규칙 기반의 특수 문자 파싱
			this.symbolParsing(in,i);
			
			//불규칙 확장 파싱
			this.irregularExpandParsing(in,i,this.observation.getTrieDictionary());
			//기본 파싱
			this.regularParsing(in,i,this.observation.getTrieDictionary());
			//불규칙 파싱
			this.irregularParsing(in,i,this.irrTrie.getTrieDictionary());
			
			//현재 character가 공백인 경우 처리 
			if(in.charAt(i) == ' '){
				//이전 탐색 노드와 다음 탐색 노드 사이에서 출현하는 공백(space)을 응용하여
				//추후 백트래킹이 가능하게 함
				this.lattice.bridgingSpace(in,i);
				lattice.setPrevStartIdx(i+1);
			}
		}
		
		//규칙 기반의 파싱 중 남은 버퍼를 lattice 결과에 삽입
		this.consumeRuleParserBuffer(in);		

		//백트래킹할 end index 설정
		this.lattice.setEndIdx(in.length());
		
		//lattice의 end index로부터 백트래킹
		if(nbest > 1){
			return this.lattice.getNbest(in);
		}else{
			List<List<List<Pair<String, String>>>> maxResult = new ArrayList<List<List<Pair<String,String>>>>();
			maxResult.add(this.lattice.getMax(in));
			return maxResult;
		}
	}

	/**
	 * 입력된 텍스트 src로부터 형태소를  <br>
	 * @param src 형태소 분석 대상 text
	 * @return
	 */
	public List<List<Pair<String, String>>> analyze(String src){
		if(src.trim().length() == 0){
			return new ArrayList<List<Pair<String, String>>>();
		}
		//형태소 분석 및 품사 태거를 위한 객체 초기화
		this.lattice = null;
		this.lattice = new Lattice(this.table);
		this.lattice.setTransition(this.transition);

		this.prevNodesRegular = null;
		this.prevNodesRegular = new PrevNodes<TrieNode<List<Pair<Integer, Double>>>>();

		this.prevNodesIrregular = null;
		this.prevNodesIrregular = new PrevNodes<TrieNode<List<IrregularNode>>>();

		this.prevNodesExpand = null;
		this.prevNodesExpand = new ArrayList<>();
		
		this.ruleMorph = "";
		this.rulePos = "";
		this.ruleBeginIdx = 0;		
		
		//자소 단위로 분리
		String in = unitParser.parse(src.trim());
		
		for(int i=0;i<in.length();i++){
			//기분석 사전 매칭
			int skipIdx = this.lookupFwd(in,i);
			//매칭된 인덱스 만큼 증가
			if(skipIdx != -1){
				i = skipIdx-1;
				continue;
			}
			
			//규칙 기반의 연속된 숫자, 영어, 한자, 외래어 파싱
			this.ruleParsing(in,i);
			//규칙 기반의 특수 문자 파싱
			this.symbolParsing(in,i);
			
			//불규칙 확장 파싱
			this.irregularExpandParsing(in,i,this.observation.getTrieDictionary());
			//기본 파싱
			this.regularParsing(in,i,this.observation.getTrieDictionary());
			//불규칙 파싱
			this.irregularParsing(in,i,this.irrTrie.getTrieDictionary());
			
			//현재 character가 공백인 경우 처리 
			if(in.charAt(i) == ' '){
				//이전 탐색 노드와 다음 탐색 노드 사이에서 출현하는 공백(space)을 응용하여
				//추후 백트래킹이 가능하게 함
				this.lattice.bridgingSpace(in,i);
				lattice.setPrevStartIdx(i+1);
			}
		}
		
		//규칙 기반의 파싱 중 남은 버퍼를 lattice 결과에 삽입
		this.consumeRuleParserBuffer(in);		

		//백트래킹할 end index 설정
		this.lattice.setEndIdx(in.length());

		
		//lattice의 end index로부터 백트래킹
		return this.lattice.getMax(in);
	}
	
	/**
	 * 입력된 문자로부터 symbol을 구분하여 lattice에 삽입
	 * @param in
	 * @param i
	 */
	private void symbolParsing(String in, int i) {
		char ch = in.charAt(i);
		Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);
		//숫자
		if(Character.isDigit(ch)){
			
		}
		else if(unicodeBlock == Character.UnicodeBlock.BASIC_LATIN){
			//영어
			if (((ch >= 'A') && (ch <= 'Z')) || ((ch >= 'a') && (ch <= 'z'))) {
				;
			}
			else if(observation.getTrieDictionary().get(ch) != null){
				;
			}
			//symbol
			else{
				this.lattice.put(i, i+1, ""+ch, this.table.getId(SYMBOL.SW), SCORE.SW);
			}
		}
		//한글
		else if(unicodeBlock == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO 
				|| unicodeBlock == UnicodeBlock.HANGUL_JAMO
				|| unicodeBlock == UnicodeBlock.HANGUL_JAMO_EXTENDED_A
				||unicodeBlock == UnicodeBlock.HANGUL_JAMO_EXTENDED_B
				||unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES){
			;
		}
		//일본어
		else if(unicodeBlock == UnicodeBlock.KATAKANA
				|| unicodeBlock == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS){
			
		}
		//중국어
		else if(UnicodeBlock.CJK_COMPATIBILITY.equals(unicodeBlock)				
				|| UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(unicodeBlock)
				|| UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(unicodeBlock)
				|| UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B.equals(unicodeBlock)
				|| UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(unicodeBlock)){
			;
		}
		else{
			this.lattice.put(i, i+1, ""+ch, this.table.getId(SYMBOL.SW), SCORE.SW);
		}
	}

	/**
	 * 규칙 기반의 파싱 중 남은 버퍼에 대해서 lattice에 삽입
	 * @param in
	 */
	private void consumeRuleParserBuffer(String in) {
		if(this.rulePos.trim().length() != 0){
			if(this.rulePos.equals("SL")){
				this.lattice.put(this.ruleBeginIdx, in.length(), this.ruleMorph, this.table.getId(this.rulePos), SCORE.SL);
			}else if(this.rulePos.equals("SH")){
				this.lattice.put(this.ruleBeginIdx, in.length(), this.ruleMorph, this.table.getId(this.rulePos), SCORE.SH);
			}else if(this.rulePos.equals("SN")){
				this.lattice.put(this.ruleBeginIdx, in.length(), this.ruleMorph, this.table.getId(this.rulePos), SCORE.SN);
			}
		}
	}

	/**
	 * 규칙 기반의 파싱 <br>
	 * 연속된 영어, 외래어, 한자, 숫자에 대해서 같은 품사가 연속된 구간에 동일한 품사 부착
	 * @param in
	 * @param i
	 */
	private void ruleParsing(String in, int i) {
		char ch = in.charAt(i);
		String curPos = "";
		if(StringUtil.isEnglish(ch)){
			curPos = "SL";
		}else if(StringUtil.isNumeric(ch)){
			curPos = "SN";
		}else if(StringUtil.isChinese(ch)){
			curPos = "SH";
		}else if(StringUtil.isForeign(ch)){
			curPos = "SL";
		}
		
		if(curPos.equals(this.rulePos)){
			this.ruleMorph += ch;
		}
		else{
			if(this.rulePos.equals("SL")){
				this.lattice.put(this.ruleBeginIdx, i, this.ruleMorph, this.table.getId(this.rulePos), SCORE.SL);
			}else if(this.rulePos.equals("SN")){
				this.lattice.put(this.ruleBeginIdx, i, this.ruleMorph, this.table.getId(this.rulePos), SCORE.SN);
			}else if(this.rulePos.equals("SH")){
				this.lattice.put(this.ruleBeginIdx, i, this.ruleMorph, this.table.getId(this.rulePos), SCORE.SH);
			}

			this.ruleBeginIdx = i;
			this.ruleMorph = ""+ch;
			this.rulePos = curPos;
		}
	}

	/**
	 * 기분석 사전 매칭
	 * @param in
	 * @param i
	 * @return
	 */
	private int lookupFwd(String in, int i) {
		if(this.fwd == null)return -1;
		if(i == 0 || in.charAt(i-1) ==' '){
			int nextSpaceIdx = in.indexOf(" ", i);
			String toMatchToken = null;
			if(nextSpaceIdx == -1){
				toMatchToken = in.substring(i);
				nextSpaceIdx = in.length();
			}else{
				toMatchToken = in.substring(i,nextSpaceIdx);
			}
			if(toMatchToken.trim().length() == 0){
				return -1;
			}
			List<Pair<String,Integer>> fwdResult = this.fwd.get(toMatchToken);
			if(fwdResult != null){
				String morph = "";
				int firstPosId = -1;
				int lastPosId = -1;
				for(int j=0;j<fwdResult.size();j++){
					Pair<String,Integer> morphPosPair = fwdResult.get(j);
					morph += morphPosPair.getFirst();
					if(j==0){
						firstPosId = morphPosPair.getSecond();
					}
					if(j== fwdResult.size()-1){
						lastPosId = morphPosPair.getSecond();
						break;
					}
					morph += "/"+this.table.getPos(morphPosPair.getSecond())+" ";
					//init
					morphPosPair = null;
				}
				morph = morph.trim();
				this.lattice.put(i, nextSpaceIdx, morph, firstPosId, lastPosId, 0.0);
				this.lattice.bridgingSpace(in,nextSpaceIdx);
				this.lattice.setPrevStartIdx(nextSpaceIdx+1);
				//init
				fwdResult = null;
				morph = null;
				return nextSpaceIdx+1;
			}else{
				return -1;
			}
		}
		return -1;
	}

	/**
	 * 이전 탐색 시 불완전 파싱된 불규칙 데이터를 확장하여 파싱
	 * @param in
	 * @param i
	 * @param trieDictionary
	 */
	private void irregularExpandParsing(String in, int i,
			TrieDictionary<List<Pair<Integer, Double>>> trieDictionary) {

		char key = in.charAt(i);

		List<Pair<Integer,IrregularNode>> tmpPrevNodesExpand = new ArrayList<>();
		for (Pair<Integer, IrregularNode> prevExtendNodes : this.prevNodesExpand) {
			int beginIdx = prevExtendNodes.getFirst();
			int endIdx = i+1;
			List<Pair<Integer,Double>> observationScoreList = trieDictionary.get(prevExtendNodes.getSecond().getLastMorph()+key);
			boolean hasChildren = trieDictionary.hasChildren();
			if(observationScoreList != null){
				for (Pair<Integer, Double> pair : observationScoreList) {
					Double innerScore = this.calIrregularScore(prevExtendNodes.getSecond().getTokens(),pair.getFirst());
					if(innerScore == null) continue;
					double score = pair.getSecond() + innerScore;
					this.lattice.put(beginIdx, endIdx, prevExtendNodes.getSecond().getMorphFormat()+key, prevExtendNodes.getSecond().getFirstPosId(), pair.getFirst(), score);

					innerScore = null;
				}
			}
			if(hasChildren){
				IrregularNode irrNode = new IrregularNode();
				irrNode.setFirstPosId(prevExtendNodes.getSecond().getFirstPosId());
				irrNode.setLastMorph(prevExtendNodes.getSecond().getLastMorph()+key);
				irrNode.setMorphFormat(prevExtendNodes.getSecond().getMorphFormat()+key);
				irrNode.setTokens(prevExtendNodes.getSecond().getTokens());
				tmpPrevNodesExpand.add(new Pair<Integer, IrregularNode>(beginIdx, irrNode));
				irrNode = null;
			}
			//init
			observationScoreList = null;
		}
		this.prevNodesExpand = tmpPrevNodesExpand;
		//init
		tmpPrevNodesExpand = null;
	}
	
	/**
	 * 불규칙의 관측 확률 및 전이 확률을 계산
	 * @param tokens
	 * @param nextPosId
	 * @return
	 */
	private Double calIrregularScore(List<Pair<String, Integer>> tokens, Integer nextPosId) {
		double score = 0.0;
		int prevPosId = -1;
		Double transitionScore = 0.0;
		for(int i=0;i<tokens.size()-1;i++){
			Pair<String,Integer> token = tokens.get(i);
			String morph = token.getFirst();
			int posId = token.getSecond();
			//관측 확률
			List<Pair<Integer,Double>> posScoreList = this.observation.getTrieDictionary().get(morph);
			for (Pair<Integer, Double> pair : posScoreList) {
				if(pair.getFirst() == posId){
					score += pair.getSecond();
					break;
				}
			}

			//전이 확률
			if(prevPosId != -1){
				transitionScore = this.transition.get(prevPosId, posId); 
				if(transitionScore == null){
					return null;
				}
				score += transitionScore;
			}
			prevPosId = posId;
			//init
			posScoreList = null;
			token = null;			
		}
		if(prevPosId != -1){
			transitionScore = this.transition.get(prevPosId, nextPosId);
		}else{
			transitionScore = null;
		}
		if(transitionScore == null){
			return null;
		}else{
			return score+transitionScore;
		}		
	}

	/**
	 * 불규칙 파싱 <br>
	 * 사전 및 이전 노드와의 결합을 통해서 lattice에 노드 삽입 <br>
	 * 불규칙 확장을 위한 내용 포함(???)<br>
	 * @param in
	 * @param i
	 * @param trieDictionary
	 */
	private void irregularParsing(String in, int i,
			TrieDictionary<List<IrregularNode>> trieDictionary) {
		char key = in.charAt(i);
		//for prev nodes
		Set<Integer> beginIdxSet = this.prevNodesIrregular.getNodeMap().keySet();
		Set<Integer> removeBeginIdxSet = new HashSet<>();
		for (Integer beginIdx : beginIdxSet) {		
			trieDictionary.setCurrentNode(this.prevNodesIrregular.get(beginIdx));
			List<IrregularNode> irregularNodeList = trieDictionary.get(key);
			if(irregularNodeList != null){
				for (IrregularNode irregularNode : irregularNodeList) {
					this.lattice.put(beginIdx, i+1, irregularNode.getMorphFormat(), irregularNode.getFirstPosId(), irregularNode.getLastPosId(), irregularNode.getInnerScore());
					this.prevNodesExpand.add(new Pair<Integer, IrregularNode>(beginIdx, irregularNode));
					if(irregularNode.getLastPosId() == this.table.getId(SYMBOL.EC)){
						this.lattice.put(beginIdx, i+1, irregularNode.getMorphFormat(), irregularNode.getFirstPosId(), this.table.getId(SYMBOL.EF), irregularNode.getInnerScore());
					}
				}
			}
			if(trieDictionary.hasChildren()){
				this.prevNodesIrregular.insert(beginIdx, trieDictionary.getCurrentNode());
			}
			else{
				removeBeginIdxSet.add(beginIdx);
			}

			//init
			irregularNodeList = null;
		}
		this.prevNodesIrregular.remove(removeBeginIdxSet);

		//for current key retrieval
		trieDictionary.setCurrentNode(null);
		List<IrregularNode> irregularNodeList = trieDictionary.get(key);
		if(irregularNodeList != null){
			for (IrregularNode irregularNode : irregularNodeList) {
				this.lattice.put(i, i+1, irregularNode.getMorphFormat(), irregularNode.getFirstPosId(), irregularNode.getLastPosId(), irregularNode.getInnerScore());
				this.prevNodesExpand.add(new Pair<Integer, IrregularNode>(i, irregularNode));
			}
		}
		if(trieDictionary.hasChildren()){
			this.prevNodesIrregular.insert(i, trieDictionary.getCurrentNode());
		}
		//init
		beginIdxSet = null;
		irregularNodeList = null;
		removeBeginIdxSet = null;
	}


	/**
	 * 일반적 파싱 <br>
	 * 이전 노드와 현재 노드를 결합 후 사전에 존재하는지 여부 파악하여 lattice에 노드 삽입<br>
	 * 단, 단일 문자 하나인 경우에도 사전에 존재한다면 lattice에 노드 삽입 가능<br> 
	 * @param in
	 * @param i
	 * @param trieDictionary
	 */
	private void regularParsing(String in, int i,TrieDictionary<List<Pair<Integer, Double>>> trieDictionary) {

		char key = in.charAt(i);

		//for prev nodes
		Set<Integer> beginIdxSet = this.prevNodesRegular.getNodeMap().keySet();
		Set<Integer> removeBeginIdxSet = new HashSet<>();
		for (Integer beginIdx : beginIdxSet) {

			trieDictionary.setCurrentNode(this.prevNodesRegular.get(beginIdx));
			List<Pair<Integer, Double>> posIdScoreList = trieDictionary.get(key);
			if(posIdScoreList != null){
				//it will be changed that insert lattice method
				this.lattice.put(beginIdx,i+1,posIdScoreList,in.substring(beginIdx, i+1));
				
				//EC를 삽입한 경우 EF도 추가적으로 삽입
				if(posIdScoreList.get(posIdScoreList.size()-1).getFirst() == this.table.getId(SYMBOL.EC)){
					List<Pair<Integer, Double>> posIdScoreListTmp = new ArrayList<Pair<Integer,Double>>();
					for (Pair<Integer, Double> pair : posIdScoreList) {
						posIdScoreListTmp.add(new Pair<Integer, Double>(pair.getFirst(),pair.getSecond()));
					}
					Pair<Integer, Double> lastIdConverted = posIdScoreListTmp.get(posIdScoreListTmp.size()-1);
					lastIdConverted.setFirst(this.table.getId(SYMBOL.EF));
					posIdScoreListTmp.set(posIdScoreListTmp.size()-1, lastIdConverted);
					this.lattice.put(beginIdx,i+1,posIdScoreListTmp,in.substring(beginIdx, i+1));
				}
				posIdScoreList = null;
			}
			if(trieDictionary.hasChildren()){
				this.prevNodesRegular.insert(beginIdx, trieDictionary.getCurrentNode());
			}
			else{
				removeBeginIdxSet.add(beginIdx);
			}
		}
		this.prevNodesRegular.remove(removeBeginIdxSet);
		//for current key
		trieDictionary.setCurrentNode(null);
		List<Pair<Integer,Double>> posIdScoreList = trieDictionary.get(key);
		if(posIdScoreList != null){
			this.lattice.put(i,i+1,posIdScoreList,in.substring(i, i+1));
		}
		if(trieDictionary.hasChildren()){
			this.prevNodesRegular.insert(i, trieDictionary.getCurrentNode());
		}

		//init
		beginIdxSet = null;
		removeBeginIdxSet = null;
		posIdScoreList = null;
	}
	
	/**
	 * 사용자 사전 추가
	 * @param userDic
	 */
	public void addUserDic(String userDic){
		try {
			BufferedReader br = new BufferedReader(new FileReader(userDic));
			String line = null;
			while((line = br.readLine()) != null){
				line = line.trim();				
				if(line.length() == 0 || line.charAt(0) == '#')continue;
				int lastIdx = line.lastIndexOf("\t");

				String morph;
				String pos;
				if(lastIdx == -1){
					morph = line.trim();
					pos = "NNP";
				}else{
					morph = line.substring(0, lastIdx);
					pos = line.substring(lastIdx+1);
				}
				this.observation.put(morph, this.table.getId(pos), 0.0);

				line = null;
				morph = null;
				pos = null;
			}
			br.close();

			//init
			br = null;
			line = null;
		} catch (Exception e) {		
			e.printStackTrace();
		}		
	}
	
	/**
	 * 사용자 사전 설정
	 * @param userDic
	 */
	public void setUserDic(String userDic) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(userDic));
			String line = null;
			while((line = br.readLine()) != null){
				line = line.trim();				
				if(line.length() == 0 || line.charAt(0) == '#')continue;
				int lastIdx = line.lastIndexOf("\t");

				String morph;
				String pos;
				if(lastIdx == -1){
					morph = line.trim();
					pos = "NNP";
				}else{
					morph = line.substring(0, lastIdx);
					pos = line.substring(lastIdx+1);
				}
				this.observation.put(morph, this.table.getId(pos), 0.0);

				line = null;
				morph = null;
				pos = null;
			}
			br.close();

			//init
			br = null;
			line = null;
		} catch (Exception e) {		
			e.printStackTrace();
		}		
	}

	/**
	 * 기분석 사전 설정
	 * @param filename
	 */
	public void setFWDic(String filename) {		
		try {
			CorpusParser corpusParser = new CorpusParser();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			this.fwd = new HashMap<String, List<Pair<String, Integer>>>();
			while ((line = br.readLine()) != null) {
				String[] tmp = line.split("\t"); //$NON-NLS-1$
				if (tmp.length != 2 || tmp[0].charAt(0) == '#'){
					tmp = null;
					continue;
				}
				ProblemAnswerPair problemAnswerPair = corpusParser.parse(line);
				List<Pair<String,Integer>> convertAnswerList = new ArrayList<>();
				for (Pair<String, String> pair : problemAnswerPair.getAnswerList()) {
					convertAnswerList.add(
							new Pair<String, Integer>(
									this.unitParser.parse(pair.getFirst()), this.table.getId(pair.getSecond())));
				}
				this.fwd.put(this.unitParser.parse(problemAnswerPair.getProblem()),
						convertAnswerList);
				tmp = null;
				problemAnswerPair = null;
				convertAnswerList = null;
			}			
			br.close();

			//init
			corpusParser = null;
			br = null;
			line = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
