package org.evosuite.seeding;

import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.statements.Statement;


import java.lang.reflect.Type;

public class MethodCalls {
    private String[] params;
    private String methodName;


    public MethodCalls(Statement statement){
        if (statement.getAccessibleObject().isMethod())
            methodName = statement.getAccessibleObject().getName();
        else if (statement.getAccessibleObject().isConstructor())
            methodName =  "<init>";

        Type[] types = statement.getAccessibleObject().getGenericParameterTypes();
        params = new String[types.length];
        for (int i=0;i<types.length;i++)
            params[i]=types[i].getTypeName();
    }

    public MethodCalls (BytecodeInstruction byteCode){

        methodName = byteCode.getCalledMethodName();

        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(byteCode.getMethodCallDescriptor());
        params = new String[argTypes.length];
        for(int i=0;i<argTypes.length;i++)
            params[i]=argTypes[i].getClassName();
    }

    public String getMethodName(){
        return methodName;
    }

    public String[] getParams(){
        return params;
    }
}
