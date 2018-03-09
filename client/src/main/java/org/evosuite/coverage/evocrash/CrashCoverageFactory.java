/**
 * Copyright (C) 2017 Mozhan Soltani, Annibale Panichella, and Arie van Deursen
 *
 * This file is part of EvoCrash.
 *
 * EvoCrash is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoCrash is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package org.evosuite.coverage.evocrash;
//import org.crash.client.CrashProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.evosuite.testsuite.AbstractFitnessFactory;



/**
 * Provides a map of fitness goals for evaluating fitness of the generated suites.
 * 
 * @author Mozhan
 *
 */
public class CrashCoverageFactory extends AbstractFitnessFactory<CrashCoverageTestFitness> {
	
    private static Map<String, CrashCoverageTestFitness> goals = new LinkedHashMap<>();
    
	Throwable targetException;

	public CrashCoverageFactory () {
		CrashProperties.getInstance();
		CrashProperties.setTargetException(CrashProperties.EXP);
		targetException = CrashProperties.TARGET_EXCEPTION;
        CrashCoverageTestFitness goal = new CrashCoverageTestFitness(targetException);
        String key = goal.getKey();
        if (!goals.containsKey(key)) {
        goals.put(key, goal);
        }
	}
	

    public static Map<String, CrashCoverageTestFitness> getGoals() {
        return goals;
    }
	
	@Override
	public ArrayList<CrashCoverageTestFitness> getCoverageGoals() {
		
		return  new ArrayList<CrashCoverageTestFitness>(goals.values());
	}
	

}