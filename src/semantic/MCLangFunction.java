package semantic;

import grtree.Tree;
import java.util.ArrayList;
import java.util.List;

public class MCLangFunction {
    public String returnType; // "PRICE", "RECIPE", "QUALITY", "EMPTY"
    public String name;
    public List<Parameter> parameters;
    public Tree bodyNode; // The <BLOCK> tree node to execute when called

    public MCLangFunction(String returnType, String name, Tree bodyNode) {
        this.returnType = returnType;
        this.name = name;
        this.bodyNode = bodyNode;
        this.parameters = new ArrayList<>();
    }

    public void addParameter(String type, String paramName) {
        this.parameters.add(new Parameter(type, paramName));
    }

    // Helper class for parameters
    public class Parameter {
        public String type;
        public String name;

        public Parameter(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}