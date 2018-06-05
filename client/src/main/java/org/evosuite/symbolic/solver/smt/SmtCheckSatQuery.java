/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.symbolic.solver.smt;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmtCheckSatQuery {

	static Logger logger = LoggerFactory.getLogger(SmtCheckSatQuery.class);

	private final List<SmtConstantDeclaration> constantDeclarations;

	private final List<SmtFunctionDeclaration> functionDeclarations;

	private final List<SmtFunctionDefinition> functionDefinitions;

	private final List<SmtAssertion> assertions;

	public SmtCheckSatQuery(List<SmtConstantDeclaration> constantDeclarations,
			List<SmtFunctionDeclaration> functionDeclarations,
			List<SmtFunctionDefinition> functionDefinitions,
			List<SmtAssertion> assertions) {
		this.constantDeclarations = constantDeclarations;
		this.functionDeclarations = functionDeclarations;
		this.functionDefinitions = functionDefinitions;
		this.assertions = assertions;
	}

	public SmtCheckSatQuery(List<SmtConstantDeclaration> constantDeclarations,
			List<SmtAssertion> assertions) {
		this(constantDeclarations, new LinkedList<SmtFunctionDeclaration>(),
				new LinkedList<SmtFunctionDefinition>(), assertions);
	}

	public SmtCheckSatQuery(List<SmtConstantDeclaration> constantDeclarations,
			List<SmtFunctionDefinition> functionDefinitions,
			List<SmtAssertion> assertions) {
		this(constantDeclarations, new LinkedList<SmtFunctionDeclaration>(),
				functionDefinitions, assertions);
	}

	public List<SmtAssertion> getAssertions() {
		return assertions;
	}

	public List<SmtConstantDeclaration> getConstantDeclarations() {
		return constantDeclarations;
	}

	public List<SmtFunctionDefinition> getFunctionDefinitions() {
		return this.functionDefinitions;
	}

	public List<SmtFunctionDeclaration> getFunctionDeclarations() {
		return this.functionDeclarations;
	}
}
