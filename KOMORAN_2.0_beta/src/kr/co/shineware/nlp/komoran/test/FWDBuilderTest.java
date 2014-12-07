package kr.co.shineware.nlp.komoran.test;

import kr.co.shineware.nlp.komoran.corpus.builder.FWDBuilder;

public class FWDBuilderTest {

	public static void main(String[] args) {
		String trainPath = "D:\\workspace_shineware\\KOMORAN_2.0_alpha_0.1\\sj2003_convert";
		FWDBuilder fwdBuilder = new FWDBuilder();
		fwdBuilder.buildPath(trainPath, "tag");
		fwdBuilder.save("user_data/fwd2.user", 2);
		fwdBuilder.save("user_data/fwd3.user", 3);
		fwdBuilder.save("user_data/fwd4.user", 4);
		fwdBuilder.save("user_data/fwd5.user", 5);
	}

}
