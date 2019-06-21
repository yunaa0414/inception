/*
 * Copyright 2019
 * Software Engineering Research Lab
 * Blekinge Institute of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.bth.serl.inception.coclasslinking.predictor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import se.bth.serl.inception.coclasslinking.recommender.CCObject;

public abstract class PredictorBase implements IPredictor {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	protected Map<String, List<CCObject>> coClassModel;
	
	public PredictorBase(Map<String, List<CCObject>> aCoClassModel) {
		coClassModel = aCoClassModel;
	}
	
	protected void logScore(Token token, Map<CCObject, Double> result) {
		StringBuffer msg = new StringBuffer();
		msg.append("Term: " + token.getText().toLowerCase());
		result.forEach((k,v) -> {
			msg.append(" / " + k.getName());
			Set<String> similarWords = k.getW2VSimilarWords(); 
			if (similarWords.size() > 0) {
				msg.append(similarWords.stream().collect(Collectors.joining(",", " [", "]")));
			}
			msg.append(" (" + v + ") ");
		});
		
		log.debug(msg.toString());
	}
}
