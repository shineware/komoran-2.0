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
package kr.co.shineware.nlp.komoran.core.analyzer.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 입력된 문자열 파싱 시(TrieDictionary 검색 시) 활용 되는 model <br>
 * 이전에 파싱된 결과(Trie node) 중 child node를 갖고 있는 경우 PrevNodes에 추가 <br>
 * 현재 입력된 문자열 뿐만 아니라 자식 노드를 갖고 있었던(아직 하위 탐색 해볼만한 가치가 있는) 이전 Trie node 들도 동일하게 하위 탐색을 함.
 * @author Junsoo Shin
 * @version 2.1
 * @since 2.1
 * @param <V>
 */
public class PrevNodes<V> {
	private Map<Integer,V> nodeMap;
	public PrevNodes(){
		this.init();
	}
	public void init(){
		nodeMap = null;
		nodeMap = new HashMap<Integer, V>();
	}
	public void insert(int beginIdx,V node){
		nodeMap.put(beginIdx, node);
	}
	public V get(int beginIdx){
		return nodeMap.get(beginIdx);
	}
	public Map<Integer, V> getNodeMap(){
		return nodeMap;
	}
	public void remove(Set<Integer> removeBeginIdxSet) {
		for (Integer beginIdxToRemove : removeBeginIdxSet) {
			nodeMap.remove(beginIdxToRemove);
		}		
	}
	public int size(){
		return nodeMap.size();
	}
}
