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

package org.evosuite.testsuite.factories;


import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.factories.RootMethodTestChromosomeFactory;
//import org.crash.client.testcase.factories.RootMethodTestChromosomeFactory;
//import org.evosuite.testsuite.CurrentChromosomeTracker;
import org.evosuite.testsuite.TestSuiteChromosome;


public class RootFixedSizeTestSuiteChromosomeFactory implements
        ChromosomeFactory<TestSuiteChromosome> {

	private static final long serialVersionUID = 6269582177138945987L;

	/** Factory to manipulate and generate method sequences */
	private final ChromosomeFactory<TestChromosome> testChromosomeFactory;

	private final int size;

	/**
	 * <p>Constructor for FixedSizeTestSuiteChromosomeFactory.</p>
	 *
	 * @param size a int.
	 */
	public RootFixedSizeTestSuiteChromosomeFactory(int size) {
		testChromosomeFactory = new RootMethodTestChromosomeFactory();
		this.size = size;
	}

	/* (non-Javadoc)
	 * @see org.evosuite.ga.ChromosomeFactory#getChromosome()
	 */
	/** {@inheritDoc} */
	@Override
	public TestSuiteChromosome getChromosome() {
		TestSuiteChromosome chromosome = new TestSuiteChromosome(
		        new RootMethodTestChromosomeFactory());
		chromosome.clearTests();
		for (int i = 0; i < size; i++) {
			TestChromosome test = testChromosomeFactory.getChromosome();
			chromosome.addTest(test);
		}
		return chromosome;
	}

}

