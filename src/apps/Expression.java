package apps;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                
    
	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    
    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() {
    	scalars = new ArrayList<ScalarSymbol>();
		arrays = new ArrayList<ArraySymbol>();
		if ((this.expr.length() == 1) && ((this.expr.charAt(0) < '0') || (this.expr.charAt(0) > '9'))) {
			ScalarSymbol tempScal = new ScalarSymbol(this.expr);
			scalars.add(tempScal);
		}
		else {	
			StringTokenizer st = new StringTokenizer(this.expr, delims);
	    	String[] tokenArray;
	    	tokenArray = new String[st.countTokens()];
	    	tokenArray = tokenize(tokenArray, st);
	    	for (int i = 0; i < tokenArray.length; i++) {
	    		if (isString(tokenArray[i])) {
	    			if (isArray(this.expr, tokenArray[i])) {
	    				ArraySymbol tempArray = new ArraySymbol(tokenArray[i]);
	        			arrays.add(tempArray);
	    			}
	    			else {
	    				ScalarSymbol tempScal = new ScalarSymbol(tokenArray[i]);
	        			scalars.add(tempScal);
	    			}
	    		}
	    	}
		}
	}
    
    private String[] removeDup(String[] strA) {
    	int numElem = strA.length;                 
        for (int i = 0; i < numElem; i++) {
            for (int j = i+1; j < numElem; j++) {                           
                if(strA[i].equals(strA[j])) {                                   
                	strA[j] = strA[numElem-1];                                       
                    numElem--;
                    j--;
                }
            }
        }                 
        String[] noDup = Arrays.copyOf(strA, numElem);
        return noDup;
    }
    
    private boolean isArray(String expr, String str) {
    	StringTokenizer st = new StringTokenizer(this.expr, delims, true);
    	String[] tokenArray;
    	tokenArray = new String[st.countTokens()];
    	tokenArray = tokenize2(tokenArray, st);
    	for (int i = 0; i < tokenArray.length - 1; i++) {
    		if (tokenArray[i].equals(str)) {
    			if (tokenArray[i+1].equals("[")) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    private String[] tokenize(String[] tokenArray, StringTokenizer st) {
    	for (int i = 0; i < tokenArray.length; i++) {
    		tokenArray[i] = st.nextToken();
    	}
    	return removeDup(tokenArray);
    }
    
    private String[] tokenize2(String[] tokenArray, StringTokenizer st) {
    	for (int i = 0; i < tokenArray.length; i++) {
    		tokenArray[i] = st.nextToken();
    	}
    	return tokenArray;
    }
    
    private boolean isString(String str) {
    	for (int i = 0; i < str.length(); i++) {
    		if (Character.isLetter(str.charAt(i)) == false) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym);
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num;
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
   
    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return Result of evaluation
     */
    public float evaluate() {
    	if (scalars.isEmpty()) {
    		return evaluate(expr.replaceAll("\\s+",""));
    	}
    	else {
    		for (int i = 0; i < scalars.size(); i++) {
    			expr = expr.replaceAll(scalars.get(i).name, Integer.toString(scalars.get(i).value));
    		}
    		return evaluate(expr.replaceAll("\\s+",""));
    	}
    }
    
    private float evaluate(String expr) {
    	if (expr.indexOf('[') != -1) {
    		return dealWithArrays(expr);
    	}
    	if (expr.indexOf('(') == -1){
    		return solve(expr);
    	}
    	else {
    		return evaluate(String.valueOf(((expr.substring(0,firstParen(expr) - 1)) + (String.valueOf(evaluate(expr.substring(firstParen(expr),closingParenthesis(expr))))) + (expr.substring(closingParenthesis(expr) + 1)))));
    	}
    }
    
    private float dealWithArrays(String expr) {
    	String whichArray = whichArray(expr);
    	int c = 0;
    	for (int i = 0; i < arrays.size(); i++) {
    		if (arrays.get(i).name.equals("A")) {
    			c = i;
    		}
    	}
    	return evaluate((expr.substring(0,firstBracket(expr) -1 - whichArray.length())) + (arrays.get(c).values[(int)evaluate(expr.substring(firstBracket(expr), closingBracket(expr)))]) + (expr.substring(closingBracket(expr) + 1)));
    }
    
    private String whichArray(String expr) {
    	StringTokenizer st = new StringTokenizer(expr, "()[+-*/", true);
    	String[] tokenArray;
    	tokenArray = new String[st.countTokens()];
    	tokenArray = tokenize2(tokenArray, st);
    	for (int i = 0; i < tokenArray.length; i++) {
    		if (tokenArray[i].equals("[")) {
    			return tokenArray[i - 1];
    		}
    	}
    	return "";
    }
    
    private int closingParenthesis(String expr) {
    	int c = 0;
    	for (int i = 0; i < expr.length(); i++) {
    		if (expr.charAt(i) == '(') {
    			c++;
    		}
    		else if (expr.charAt(i) == ')') {
    			c--;
    			if (c == 0) {
    				return i;
    			}
    		}
    	}
    	return -1;
    }
    
    private int closingBracket(String expr) {
    	int c = 0;
    	for (int i = 0; i < expr.length(); i++) {
    		if (expr.charAt(i) == '[') {
    			c++;
    		}
    		else if (expr.charAt(i) == ']') {
    			c--;
    			if (c == 0) {
    				return i;
    			}
    		}
    	}
    	return -1;
    }
    
    private int firstParen(String expr) {
    	return expr.indexOf('(') + 1;
    }
    
    private int firstBracket(String expr) {
    	return expr.indexOf('[') + 1;
    }
    
    
    private float solve(String expr) {
    	Stack<Character> operators = new Stack<Character>();
    	Stack<Float> operands = new Stack<Float>();
    	Stack<Character> resultsOperators = new Stack<Character>();
    	Stack<Float> resultsOperands = new Stack<Float>();
    	StringTokenizer st = new StringTokenizer(expr, "+-*/", true);
    	String[] tokenArray;
    	tokenArray = new String[st.countTokens()];
    	tokenArray = tokenize2(tokenArray, st);
    	if (tokenArray.length == 2 && tokenArray[0].equals("-")) {
    		operands.push(-1 * Float.valueOf(tokenArray[1]));
    	}
    	else {
	    	for (int i = 0; i < tokenArray.length; i++) {
	    		if (i != tokenArray.length - 1) {
	    			if (tokenArray[i].equals("-") && (tokenArray[i + 1].equals("-"))){
	    				operators.push('+');
	    				i = i + 2;
	    			}		
	    		}
	    		
	    		if (tokenArray[i].equals("+")  || tokenArray[i].equals("-") || tokenArray[i].equals("*") || tokenArray[i].equals("/")) {
	    			operators.push(tokenArray[i].charAt(0));
	    		}
	    		else {
	    			operands.push(Float.valueOf(tokenArray[i]));
	    		}
	    	}
    	}
    	return recSolve(operators,operands,resultsOperators,resultsOperands); 
    }
    
    private float recSolve(Stack<Character> operators,Stack<Float> operands, Stack<Character> resultsOperators, Stack<Float> resultsOperands) {
    	if (operators.isEmpty()) {
    		resultsOperands.push(operands.pop());
    		return finalSolve(resultsOperators,resultsOperands);
    	}
    	else if (operators.peek() == '+' || operators.peek() == '-') {
    		resultsOperators.push(operators.pop());
    		resultsOperands.push(operands.pop());
    		return recSolve(operators,operands,resultsOperators,resultsOperands);
    	}
    	else if(operators.peek() == '*') {
    		operands.push(operands.pop() * operands.pop());
    		operators.pop();
    		return recSolve(operators,operands,resultsOperators,resultsOperands);
    	}
    	else if(operators.peek() == '/') {
    		Stack<Float> temp = new Stack<Float>();
    		temp.push(operands.pop());
    		operands.push(operands.pop() / temp.pop());
    		operators.pop();
    		return recSolve(operators,operands,resultsOperators,resultsOperands);
    	}
    	return -1;
    }
    
    private float finalSolve(Stack<Character> resultsOperators, Stack<Float> resultsOperands) {
    	if (resultsOperators.isEmpty()) {
    		return resultsOperands.peek();
    	}
    	else if (resultsOperators.peek() == '+') {
    		resultsOperands.push(resultsOperands.pop() + resultsOperands.pop());
    		resultsOperators.pop();
    		return finalSolve(resultsOperators,resultsOperands);
    	}
    	else if (resultsOperators.peek() == '-') {
    		Stack<Float> temp = new Stack<Float>();
    		temp.push(resultsOperands.pop());
    		resultsOperands.push(temp.pop() - resultsOperands.pop());
    		resultsOperators.pop();
    		return finalSolve(resultsOperators,resultsOperands);
    	}
    	return -1;
    }

    /**
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    		for (ArraySymbol as: arrays) {
    			System.out.println(as);
    		}
    }

}
