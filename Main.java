
/*Name: Rayhan Hossain
NedId: rhossai2
Course: COSC461-Compilers
Assignment: PA_1
Date: 09-08-2017*/

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class Main {

	// File should be placed on main project folder
	private static String FILENAME = "input1.txt";
	private static String nfaInitialState = "";
	private static int nfaTotalStates = 0;
	private static List<String> nfaFinalStateNames = new ArrayList<String>();
	private static List<String> pathValues = new ArrayList<String>();
	private static int pathValueSize = 0;
	private static List<NFAState> nfaStates = new ArrayList<NFAState>();
	private static List<List<String>> dfaStateNames = new ArrayList<List<String>>();
	private static List<List<Integer>> dfaPathTrack = new ArrayList<List<Integer>>();

	public static void main(String[] args) {

		//checking if the user puts the fileName with command line
		if(args.length > 0){
			FILENAME = args[0];
		}
		Main mainClass = new Main();

		BufferedReader br = null;
		FileReader fr = null;
		try {

			fr = new FileReader(FILENAME);
			br = new BufferedReader(fr);

			String currentLine;
			int lineCounter = 1;

			//parsing the input and storing into appropriate data structure
			while ((currentLine = br.readLine()) != null) {

				//if the line number is 1 it will be initial state
				if (lineCounter == 1) {
					String[] splitArray = currentLine.split(":");
					nfaInitialState = splitArray[1].trim();
				} else if (lineCounter == 2) { //if the line number is 2 it will be final state list
					String[] splitArray = currentLine.split(":");
					String finalStates = splitArray[1].trim();
					StringTokenizer st = new StringTokenizer(finalStates,"{//}//,");
					while (st.hasMoreTokens()) {
						String cState = st.nextToken();
						if (cState != null && cState.length() > 0) {
							nfaFinalStateNames.add(cState.trim());
						}
					}
				} else if (lineCounter == 3) { //if the line number is 3 it will be state and path names
					String[] splitArray = currentLine.split(":");
					nfaTotalStates = Integer.parseInt(splitArray[1].trim());
				} else if (lineCounter == 4) {
					String[] splitArray = currentLine.split("\\s+");
					pathValueSize = splitArray.length - 1;
					for (int i = 1; i < splitArray.length; i++) {
						pathValues.add(splitArray[i].trim());
					}
				} else { //otherwise all the line contains the path connection info between nodes
					String[] splitArray = currentLine.split("\\s+");

					NFAState nfaState = new NFAState(splitArray[0].trim());
					nfaState.adjecencyStatesForPath = new HashMap<String, List<String>>();

					for (int j = 1; j < splitArray.length; j++) {
						List<String> adjStateList = new ArrayList<String>();

						String adjStates = splitArray[j].trim();
						StringTokenizer st = new StringTokenizer(adjStates,"{//}//,");

						while (st.hasMoreTokens()) {
							String nextState = st.nextToken();
							if (nextState != null && nextState.length() > 0) {
								adjStateList.add(nextState.trim());
							}
						}
						nfaState.adjecencyStatesForPath.put(pathValues.get(j - 1), adjStateList);
					}

					nfaStates.add(nfaState);
				}

				lineCounter++;
			}

			br.close();
		} catch (IOException e) {
			System.out.println("Please check the file path...");
			e.printStackTrace();
			return;
		}

		System.out.println("reading NFA ... done.\n");

		System.out.println("creating corresponding DFA ...");
		mainClass.nfa2dfa(nfaInitialState);
		for (int k = 0; k < dfaStateNames.size(); k++) {
			System.out.print("new DFA state:  " + (k + 1));
			if ((k + 1) < 10) {
				System.out.print("    -->  {");
			} else if ((k + 1) >= 10 && (k + 1) <= 99) {
				System.out.print("   -->  {");
			} else if ((k + 1) >= 100 && (k + 1) <= 999) {
				System.out.print("  -->  {");
			} else {
				System.out.print(" -->  {");
			}
			List<Integer> nodeList = mainClass
					.getIntListFromStringList(dfaStateNames.get(k));

			int counter = 0;
			for (Integer s : nodeList) {
				if (counter > 0)
					System.out.print(",");
				System.out.print(s);
				counter++;
			}
			System.out.println("}");
		}
		System.out.println("done\n\nfinal DFA:");
		System.out.println("Initial State:  1");
		System.out.print("Final States:   {");
		int sCounter = 0;
		for (int l = 0; l < dfaStateNames.size(); l++) {
			if (mainClass.isFinalState(dfaStateNames.get(l))) {
				if (sCounter > 0)
					System.out.print(",");
				System.out.print((l + 1));

				sCounter++;
			}

		}
		System.out.println("}");

		System.out.println("Total States:   " + dfaStateNames.size());

		System.out.print("State\t");
		for (int s = 0; s < pathValues.size() - 1; s++) {

			System.out.print(pathValues.get(s) + "\t\t");
		}
		System.out.println();

		for (int p = 0; p < dfaPathTrack.size(); p++) {
			List<Integer> toStateList = dfaPathTrack.get(p);
			System.out.print((p + 1) + "\t");
			for (Integer in : toStateList) {
				System.out.print("{");
				if (in > 0)
					System.out.print(in);

				System.out.print("}\t\t");
			}
			System.out.println();
		}
	}

	// this method will do the main task for converting a NFA into DFA
	void nfa2dfa(String startNode) {
		Queue<List<String>> queue = new LinkedList<List<String>>();

		List<String> nodeList = new ArrayList<String>();
		nodeList.add(startNode);
		List<String> eClosureFirst = getEClosure(nodeList);

		queue.add(eClosureFirst);

		dfaStateNames.add(eClosureFirst);

		while (!queue.isEmpty()) {
			List<String> sList = queue.remove();

			List<Integer> pathList = new ArrayList<Integer>();
			for (int i = 0; i < pathValueSize - 1; i++) {
				List<String> adjNodes = new ArrayList<String>();
				String key = pathValues.get(i);
				for (String s : sList) {
					NFAState startState = new NFAState();
					for (NFAState nfa : nfaStates) {
						if (nfa.getName().equals(s)) {
							startState = nfa;
						}
					}
					List<String> adjList = startState.adjecencyStatesForPath
							.get(key);
					for (String st : adjList) {
						if (!adjNodes.contains(st)) {
							adjNodes.add(st);
						}
					}
				}

				int isPath = -1;
				List<String> eClList = getEClosure(adjNodes);
				int dfaIndex = isInDFAList(eClList);

				if (dfaIndex == -1 && eClList.size() > 0) {
					dfaStateNames.add(eClList);
					queue.add(eClList);
					isPath = dfaStateNames.size();
				} else {
					isPath = dfaIndex + 1;
				}

				pathList.add(isPath);
			}
			dfaPathTrack.add(pathList);

		}
	}

	// this method will check whether a DFA state is already in the list or not
	int isInDFAList(List<String> list) {
		for (int i = 0; i < dfaStateNames.size(); i++) {
			if (dfaStateNames.get(i).containsAll(list)
					&& dfaStateNames.get(i).size() == list.size())
				return i;
		}
		return -1;
	}

	// this method will check whether a DFA state is a final state or not
	boolean isFinalState(List<String> stateNames) {
		for (String s : stateNames) {
			if (nfaFinalStateNames.contains(s))
				return true;
		}
		return false;
	}

	//this method will generate the e-closure list of a given node list
	List<String> getEClosure(List<String> states) {
		List<String> eClosureList = new ArrayList<String>();
		Stack<String> stack = new Stack<String>();

		for (String s : states) {
			stack.push(s);
			eClosureList.add(s);
		}
		while (!stack.isEmpty()) {
			String s = stack.pop();
			NFAState startState = new NFAState();
			for (NFAState nfa : nfaStates) {
				if (nfa.getName().equals(s)) {
					startState = nfa;
				}
			}
			String eKey = pathValues.get(pathValueSize - 1);
			List<String> adjList = startState.adjecencyStatesForPath.get(eKey);

			for (String adj : adjList) {
				if (!eClosureList.contains(adj)) {
					eClosureList.add(adj);
					stack.push(adj);
				}
			}
		}

		return eClosureList;
	}

	//for converting a string list into an integer list
	List<Integer> getIntListFromStringList(List<String> stList) {
		List<Integer> intList = new ArrayList<Integer>();
		for (String s : stList) {
			int x = Integer.parseInt(s);
			intList.add(x);
		}
		Collections.sort(intList);
		return intList;
	}
}

class NFAState {
	public String name;
	Map<String, List<String>> adjecencyStatesForPath;

	public NFAState(String name) {
		this.name = name;
	}

	public NFAState() {
	}

	public String getName() {
		return this.name;
	}
}

