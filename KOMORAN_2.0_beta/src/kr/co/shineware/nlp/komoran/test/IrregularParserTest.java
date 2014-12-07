package kr.co.shineware.nlp.komoran.test;

import java.util.List;

import kr.co.shineware.nlp.komoran.corpus.model.Dictionary;
import kr.co.shineware.nlp.komoran.corpus.parser.IrregularParser;
import kr.co.shineware.nlp.komoran.parser.KoreanUnitParser;
import kr.co.shineware.util.common.file.FileUtil;
import kr.co.shineware.util.common.model.Pair;
import kr.co.shineware.util.common.string.StringUtil;

public class IrregularParserTest {

	public static void main(String[] args) {		
//		String line = "너라고\t너/NP 이/VCP 라고/EC";
//		String line = "하얘서\t하얗/VA 아서/EC";
//		String line = "하얘진\t하얗/VA 아/EC 지/VX ㄴ/ETM";
//		String line = "누군지\t누구/VV 인지/EC"; //error
//		String line = "부어서\t붓/VV 어서/EC";
		unitTest();
//		trainSetTest();	
		
	}

	private static void unitTest() {
		IrregularParser parser =new IrregularParser();
		KoreanUnitParser unitParser = new KoreanUnitParser();
//		String line = "찾으시면\t찾/VV 으시/EP 시/EP 면/EC";
//		String line = "햇\t해/NNG";
//		String line = "알리가\t알/1 ㄹ/2 리가/2";
//		String line = "안다\t알/1 ㄴ다/2";
//		String line = "부어서\t붓/VV 어서/EC";
//		String line = "너라고\t너/NP 이/VCP 라고/EC";
//		String line = "너라고\t너/NP 라고/EC 이/AD";
//		String line = "나가서\t나가/NP 어서/VCP";
//		String line = "흘렸다\t흐르/NP 었/VCP 다/EF";
//		String line = "라고\tㄹ/NP 라고/EF";
//		String line = "알\t알/VV ㄹ/ETM";
		String line = "공무원에ㄴ	공무원/NNG 직/NNG 에/JKB";
		for(int i=0;i<line.length();i++){
			System.out.println(StringUtil.getUnicodeBlock(line.charAt(i)));
		}
//		System.out.println();
		line = unitParser.parse(line);
		System.out.println(line);
		System.out.println(parser.parse(line));
	}

	public static void trainSetTest() {
		IrregularParser parser =new IrregularParser();
		KoreanUnitParser unitParser = new KoreanUnitParser();
		String trainPathForWindow2 = "D:\\workspace_shineware\\KOMORAN_2.0_alpha_0.1\\sj2003_convert";
//		String trainPathForWindow2 = "D:\\00_Workspace\\shineware\\KOMORAN_2.0_alpha_0.1\\sj2003_convert";
		List<String> filenames = FileUtil.getFileNames(trainPathForWindow2,"tag");
		Dictionary dic = new Dictionary();
		for (String filename : filenames) {
			System.out.println(filename);
			List<String> lines = FileUtil.load2List(filename);
			for (String line : lines) {
				if(line.trim().length() == 0)continue;
				line = unitParser.parse(line);
				List<Pair<String,String>> irrRuleList = parser.parse(line);
				if(irrRuleList == null)continue;
				for (Pair<String, String> irrRulePair : irrRuleList) {
					dic.append(irrRulePair.getFirst(), irrRulePair.getSecond());
				}
			}
		}
		dic.save("dic.irr");
	}

}
