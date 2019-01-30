package skku.selab.staticanalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.utils.Pair;

import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import skku.selab.staticanalysis.utils.FileUtils;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	Options options = new Options();

        Option srcDir = new Option("s", "srcdir", true, "source root dir");
        srcDir.setRequired(true);
        options.addOption(srcDir);

        Option patchDir = new Option("p", "patchdir", true, "patch root dir");
        patchDir.setRequired(true);
        options.addOption(patchDir);
        
        Option codeFilePath = new Option("f", "file", true, "source code file");
        codeFilePath.setRequired(true);
        options.addOption(codeFilePath);
        
        Option outdir = new Option("o", "outdir", true, "output root dir");
        outdir.setRequired(true);
        options.addOption(outdir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
    	
    	//
    	/**dir structure
    	 * 	AprName_BugId_PatchId
    	 * 	-source	
    	 * 		--buggy
    	 * 			--code1.java	
    	 * 		--patch
    	 * 			--code1.java
    	 * 	-snippet
    	 * 		--bugsnippets
    	 * 			--code1.java.snippet0
    	 * 			--code1.java.snippet1
    	 * 		--patchsnippets
    	 * 			--code1.java.snippet0
    	 * 			--code1.java.snippet1
    	 */
    	
    	AstComparator comparator = new AstComparator(); //.compare((CtElement) el1, (CtElement) el2);
    	
    	String projectName = "chart";
    	int bugId = 14;
    	String d4jProjectDir = "/home/ngocpq/projects";    	
    	String bugSourceDir = d4jProjectDir+File.separator+projectName+File.separator+projectName+"_"+bugId+File.separator+"source";
    	
    	String patchId = "ASC_patch1";
    	
    	String patchSourceDir = "/home/ngocpq/patches/patch/ACS/Chart14/source";
    	String filePath = "org/jfree/chart/plot/CategoryPlot.java";
//    	String filePath = "org/jfree/chart/plot/XYPlot.java";	
    	
    	String bugFilePath = bugSourceDir+File.separator+filePath;
    	String patchFilePath = patchSourceDir+File.separator+filePath;
    	
    	String snippetRootDir = "/home/ngocpq/patches/snippets";
    	String outputDir = snippetRootDir+File.separator+projectName+bugId+"_"+patchId;    	
    	
    	System.out.println(bugFilePath);
    	System.out.println(patchFilePath);
		File fBug = new File(bugFilePath);
		File fPatch = new File(patchFilePath);
		
		
		Diff diff = comparator.compare(fBug, fPatch);		
		
		List<Pair<CtElement, CtElement>> diffPairs = new ArrayList<Pair<CtElement,CtElement>>();
		List<List<Operation<?>>> diffOperations = new ArrayList<List<Operation<?>>>(); 
		int count=0;
    	for(Operation<?> op : diff.getRootOperations()){
    		System.out.println(count+": " + op);
    		CtElement srcNode = op.getSrcNode();
    		//CtElement dstNode = op.getDstNode();
    		
    		boolean isFromSource = true; 
    		if (op.getAction() instanceof Addition){
    			isFromSource=false;	
    		}
    		
    		System.out.println("--- srcNode ---");
    		System.out.println(srcNode);
    		if (srcNode.getPosition() != null && !(srcNode.getPosition() instanceof NoSourcePosition)){    			
    			System.out.println("srcPos: "+srcNode.getPosition().getLine() +" to " +srcNode.getPosition().getEndLine());    			
    			//findLine
    		}else
    			System.out.println("no src position");
    		//get method context
    		System.out.println("srcPath: \n"+srcNode.getPath());
    		
    		//CtElement<?> methodSrc = srcNode.getParent(CtMethod.class);
    		CtElement methodSrc = getContainCodeSnippet(srcNode);
    		    		
    		gumtree.spoon.diff.support.SpoonSupport support = new gumtree.spoon.diff.support.SpoonSupport();
    		CtElement methodTgt = support.getMappedElement(diff, methodSrc, isFromSource);
    		
    		if (!isFromSource){
    			CtElement tmp = methodSrc;
    			methodSrc=methodTgt;
    			methodTgt = tmp;
    		}
    		Pair<CtElement, CtElement> pair = new Pair<CtElement, CtElement>(methodSrc, methodTgt);    		
			System.out.println("--- source Method: ---");
			System.out.println(methodSrc);
			System.out.println("--- target Method: ---");
			System.out.println(methodTgt);
			int index = diffPairs.indexOf(pair);
			List<Operation<?>> operationsList ;
			if (index==-1){
    			diffPairs.add(pair);
    			operationsList = new ArrayList<Operation<?>>();
    			diffOperations.add(operationsList);
    			index = diffOperations.size()-1;
    		}else{
    			operationsList = diffOperations.get(index);
    		}
			operationsList.add(op);			
			count++;
    	}
    	System.out.println("DiffPairs size: "+diffPairs.size());
    	
    	//write code snippet to files
    	String patchedSnippetDir = outputDir+File.separator+"patched";
    	String originSnippetDir = outputDir+File.separator+"origin";
    	for(int i=0;i<diffPairs.size();i++){
    		Pair<CtElement, CtElement> pair = diffPairs.get(i);
    		//List<Operation<?>> operations = diffOperations.get(i);    		
    		String snippetFilePath = filePath+".snippet"+i;
    		String patchedCodeSnippetFile = patchedSnippetDir+File.separator+snippetFilePath;
    		String originCodeSnippetFile = originSnippetDir+File.separator+snippetFilePath;
    		//write to file
    		String origCode = pair.first.toString();    		
    		try{
	    		FileUtils.writeToFile(originCodeSnippetFile,origCode);
    		}catch (IOException ex){
    			System.out.println("Error while writing origin code snippet: "+ ex.getMessage());
    			System.out.println(origCode);
    		}
    		
    		String patchedCode = pair.second.toString();
    		try{
    			FileUtils.writeToFile(patchedCodeSnippetFile,patchedCode);
    		}catch (IOException ex){
    			System.out.println("Error while writing patched code snippet: "+ ex.getMessage());
    			System.out.println(patchedCode);
    		}
    	}    	
    }

	private static CtElement getContainCodeSnippet(CtElement srcNode) {
		CtElement parent = srcNode;
		while (parent!=null){
			if (parent instanceof CtMethod)
				return parent;
			if (parent instanceof CtConstructor)
				return parent;
			if (parent instanceof CtField)
				return parent;
			parent = parent.getParent();
		}
		return null;
	}
    
    
}
