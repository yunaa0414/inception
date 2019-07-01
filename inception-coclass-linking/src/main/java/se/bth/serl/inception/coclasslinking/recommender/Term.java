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

package se.bth.serl.inception.coclasslinking.recommender;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class Term
{
    private String term;
    private String stem;
    private String posValue;

    public Term(Token aToken)
    {
        term = aToken.getText();
        stem = aToken.getStemValue();
        posValue = aToken.getPosValue();
    }

    public Term(String aText)
    {
        term = aText;
        stem = aText;
        posValue = "";
    }

    public String getTerm()
    {
        return term.toLowerCase();
    }

    public String getStem()
    {
        return stem.toLowerCase();
    }

    /*
     * Identifiers are from Talbanken76, which is used by the OpenNLP POS tagger
     * https://cl.lingfil.uu.se/~nivre/research/Talbanken05.html
     */
    public boolean isNoun()
    {
        return posValue.equals("PN") || posValue.equals("MN") || posValue.equals("AN")
                || posValue.equals("VN") || posValue.equals("NN");
    }
}
