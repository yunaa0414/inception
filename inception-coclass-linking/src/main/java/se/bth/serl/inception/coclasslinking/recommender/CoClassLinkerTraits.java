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

import java.io.Serializable;

public class CoClassLinkerTraits
    implements Serializable
{
    private static final long serialVersionUID = 6514924713945451020L;
    
    private int maxRejects = 3;
    private double minConfidence = 0.05;
    
    public int getMaxRejects()
    {
        return maxRejects;
    }
    
    public void setMaxRejects(int maximumRejects)
    {
        this.maxRejects = maximumRejects;
    }
    
    public double getMinConfidence()
    {
        return minConfidence;
    }
    
    public void setMinConfidence(double minimumConfidence)
    {
        this.minConfidence = minimumConfidence;
    }
}
