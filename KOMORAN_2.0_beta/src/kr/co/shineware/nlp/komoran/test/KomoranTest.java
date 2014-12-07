package kr.co.shineware.nlp.komoran.test;

import java.util.List;

import kr.co.shineware.nlp.komoran.core.analyzer.Komoran;
import kr.co.shineware.util.common.model.Pair;

public class KomoranTest {

	public static void main(String[] args) {
		Komoran komoran = new Komoran("models");
		komoran.setUserDic("user_data/dic.user");
		long start = System.currentTimeMillis();
		String in = "바람과 함께 사라지다를 봤다.";
		//		String in = "어떠신지";

		List<List<Pair<String, String>>> analyzeResultList = komoran.analyze(in);
		System.out.println("line : "+in);
		for (List<Pair<String, String>> wordResultList : analyzeResultList) {
			for(int i=0;i<wordResultList.size();i++){
				Pair<String, String> pair = wordResultList.get(i);
				System.out.print(pair.getFirst()+"/"+pair.getSecond());
				if(i != wordResultList.size()-1){
					System.out.print("+");
				}
			}
			System.out.println();
		}
		System.out.println(System.currentTimeMillis() - start+" ms");
		System.out.println();

		List<List<List<Pair<String, String>>>> analyzeResultList2 = komoran.analyzeWithoutSpace(in,4);
		System.out.println("line : "+in);
		for (List<List<Pair<String, String>>> analyzeResultList3 : analyzeResultList2) {


			for (List<Pair<String, String>> wordResultList : analyzeResultList3) {
				for(int i=0;i<wordResultList.size();i++){
					Pair<String, String> pair = wordResultList.get(i);
					System.out.print(pair.getFirst()+"/"+pair.getSecond());
					if(i != wordResultList.size()-1){
						System.out.print("+");
					}
				}
				System.out.println();			
			}
			System.out.println();
		}
		System.out.println(System.currentTimeMillis() - start+" ms");

	}

}
