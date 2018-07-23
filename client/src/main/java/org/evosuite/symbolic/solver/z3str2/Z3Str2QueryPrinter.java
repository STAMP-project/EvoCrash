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
package org.evosuite.symbolic.solver.z3str2;

import org.evosuite.symbolic.solver.smt.SmtAssertion;
import org.evosuite.symbolic.solver.smt.SmtCheckSatQuery;
import org.evosuite.symbolic.solver.smt.SmtConstantDeclaration;
import org.evosuite.symbolic.solver.smt.SmtExpr;
import org.evosuite.symbolic.solver.smt.SmtExprPrinter;
import org.evosuite.symbolic.solver.smt.SmtFunctionDefinition;

class Z3Str2QueryPrinter {

	public String print(SmtCheckSatQuery smtQuery) {

		StringBuffer buff = new StringBuffer();
		buff.append("\n");

		for (SmtConstantDeclaration constantDeclaration : smtQuery
				.getConstantDeclarations()) {
			String str = String.format("(declare-const %s %s)",
					constantDeclaration.getConstantName(),
					constantDeclaration.getConstantSort());
			buff.append(str);
			buff.append("\n");
		}

		for (SmtFunctionDefinition smtFunctionDefinition : smtQuery
				.getFunctionDefinitions()) {
			String str = String.format("(define-fun %s)",
					smtFunctionDefinition.getFunctionDefinition());
			buff.append(str);
			buff.append("\n");
		}

		SmtExprPrinter printer = new SmtExprPrinter();
		for (SmtAssertion assertionDeclaration : smtQuery.getAssertions()) {

			SmtExpr formula = assertionDeclaration.getFormula();
			String formulaStr = formula.accept(printer, null);
			String str = String.format("(assert %s)", formulaStr);
			buff.append(str);
			buff.append("\n");
		}

		buff.append("(check-sat)");
		buff.append("\n");

		return buff.toString();
	}
}
