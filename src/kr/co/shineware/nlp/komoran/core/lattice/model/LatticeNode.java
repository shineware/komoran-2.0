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
package kr.co.shineware.nlp.komoran.core.lattice.model;


public class LatticeNode {
	private String morph;
	private int posId;
	private double score;
	private int prevIdx;
	private int prevHashcode;
	
	public String getMorph() {
		return morph;
	}
	public void setMorph(String morph) {
		this.morph = morph;
	}
	public int getPosId() {
		return posId;
	}
	public void setPosId(int posId) {
		this.posId = posId;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public int getPrevIdx() {
		return prevIdx;
	}
	public void setPrevIdx(int prevIdx) {
		this.prevIdx = prevIdx;
	}
	public int getPrevHashcode() {
		return prevHashcode;
	}
	public void setPrevHashcode(int prevHashcode) {
		this.prevHashcode = prevHashcode;
	}
	@Override
	public String toString() {
		return "LatticeNode [morph=" + morph + ", posId=" + posId + ", score="
				+ score + ", prevIdx=" + prevIdx + ", prevHashcode="
				+ prevHashcode + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((morph == null) ? 0 : morph.hashCode());
		result = prime * result + posId;
		result = prime * result + prevHashcode;
		result = prime * result + prevIdx;
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatticeNode other = (LatticeNode) obj;
		if (morph == null) {
			if (other.morph != null)
				return false;
		} else if (!morph.equals(other.morph))
			return false;
		if (posId != other.posId)
			return false;
		if (prevHashcode != other.prevHashcode)
			return false;
		if (prevIdx != other.prevIdx)
			return false;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		return true;
	}
	
}
