package kr.co.shineware.nlp.komoran.test;

import java.util.List;

import kr.co.shineware.nlp.komoran.core.analyzer.Komoran;
import kr.co.shineware.nlp.komoran.modeler.builder.ModelBuilder;
import kr.co.shineware.util.common.model.Pair;

public class KomoranExample {

	public static void main(String[] args) {
		
		
		//corpus_build에 있는 데이터로부터 models 생성
		ModelBuilder builder = new ModelBuilder();
		builder.buildPath("corpus_build");
		builder.save("models");
		
		
		//생성된 models를 이용하여 객체 생성
		Komoran komoran = new Komoran("models");
		//사용자 사전 추가
		komoran.setUserDic("user_data/dic.user");
		
		String in = "바람과 함께 사라지다를 봤다.";
		
		List<List<Pair<String, String>>> analyzeResultList = komoran.analyze(in);
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
	}

}
