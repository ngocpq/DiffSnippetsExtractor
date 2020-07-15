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
import org.apache.commons.lang3.EnumUtils;

import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
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
import spoon.support.sniper.internal.ElementSourceFragment;

public class DiffSnippetExtractor {
	static LogLevel logLevel = LogLevel.info;
	
	public static void main( String[] args ) throws Exception
    {
		/*args=new String[]{
			"-f","org/jfree/chart/plot/CategoryPlot.java",
			"-s","/home/ngocpq/projects/chart/chart_14/source",
			"-p","/home/ngocpq/patches/patch/ACS/Chart14/source",
			"-o","/home/ngocpq/patches/snippets/chart14_ASC_patch1",
		};*/
		
		/*args=new String[]{
			//"-f","java_programs/Node.java",
			"-s","C:/VirtualMachines/SharedFolder/Defects4J/Cli1_Developer_p1/original",
			"-p","C:/VirtualMachines/SharedFolder/Defects4J/Cli1_Developer_p1/patched",
			"-o","C:/VirtualMachines/SharedFolder/Defects4J/test_snippets/Cli1_Developer_p1_test",
			"-l","info"
		};*/
    	Options options = new Options();

        Option srcDir = new Option("s", "srcdir", true, "source root dir");
        srcDir.setRequired(true);
        options.addOption(srcDir);

        Option patchDir = new Option("p", "patchdir", true, "patch root dir");
        patchDir.setRequired(true);
        options.addOption(patchDir);
        
        Option outdir = new Option("o", "outdir", true, "output root dir");
        outdir.setRequired(true);
        options.addOption(outdir);
        
        Option optFilePath = new Option("f", "file", true, "source code file");
        optFilePath.setRequired(false);
        options.addOption(optFilePath);       
        
        //.[jJ]ava$
        Option optRegexPattern = new Option("r", "regex", true, "regex file pattern");
        optRegexPattern.setRequired(false);
        options.addOption(optRegexPattern);
        
        Option optLogLever = new Option("l", "log", true, "log level");
        optLogLever.setRequired(false);
        options.addOption(optLogLever);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
            return;
        }
    	
    	//
    	/**snippet
    	 * 		--origin
    	 * 			--code1.java.snippet0
    	 * 			--code1.java.snippet1
    	 * 		--patched
    	 * 			--code1.java.snippet0
    	 * 			--code1.java.snippet1
    	 */
    	
    	String srcDirPath = cmd.getOptionValue(srcDir.getOpt());    	
    	String patchDirPath = cmd.getOptionValue(patchDir.getOpt());
    	String filePath = cmd.getOptionValue(optFilePath.getOpt());	
    	String outputDir = cmd.getOptionValue(outdir.getOpt());    	    	    	
    	    	
    	if (cmd.hasOption(optLogLever.getOpt())) {    		
    		String level= cmd.getOptionValue(optLogLever.getOpt()).toLowerCase();
    		if (EnumUtils.isValidEnum(LogLevel.class, level)) {
    			logLevel = LogLevel.valueOf(level);
    		}
    	}
    	
    	if (cmd.hasOption(optFilePath.getOpt()))    		
    		genDiffCodeSnippetForFile(srcDirPath, patchDirPath, filePath, outputDir);
    	else {
    		String regString = ".*?[Jj]ava$";
    		if (cmd.hasOption(optRegexPattern.getOpt()))
    			regString = cmd.getOptionValue(optRegexPattern.getOpt());
    		genDiffCodeSnippetForDir(srcDirPath, patchDirPath, outputDir,regString);
    	}
    	
    	System.exit(0);
    }
	
	enum LogLevel{
		error,
		warning,
		info,
		fine,
		finer,
		finest
	}

	private static void genDiffCodeSnippetForDir(String srcDirPath, String patchDirPath,String outputDir,String regexStr) throws Exception {
		 String[] filePaths = FileUtils.getAllFilesInFolder(patchDirPath,regexStr);
		 for(String file:filePaths){
			 genDiffCodeSnippetForFile(srcDirPath, patchDirPath, file, outputDir);
		 }
	}
	
	private static void genDiffCodeSnippetForFile(String srcDirPath, String patchDirPath, String filePath,
			String outputDir) throws Exception {
		String bugFilePath = srcDirPath+File.separator+filePath;
    	String patchFilePath = patchDirPath+File.separator+filePath;    	    	
    	writeLog(LogLevel.fine,bugFilePath);
    	writeLog(LogLevel.fine,patchFilePath);		
		
    	File fBug = new File(bugFilePath);
		File fPatch = new File(patchFilePath);		
		AstComparator comparator = new AstComparator(); //.compare((CtElement) el1, (CtElement) el2);    	
		Diff diff = comparator.compare(fBug, fPatch);		
		
		//System.out.println("Result:");
		//System.out.println(diff.toString());
		
		List<Pair<CtElement, CtElement>> diffPairs = new ArrayList<Pair<CtElement,CtElement>>();
		List<List<Operation<?>>> diffOperations = new ArrayList<List<Operation<?>>>(); 
		int count=0;
    	for(Operation<?> op : diff.getRootOperations()){
    		writeLog(LogLevel.finer,count+": " + op);
    		CtElement srcNode = op.getSrcNode();
    		//CtElement dstNode = op.getDstNode();
    		
    		boolean isFromSource = true; 
    		if (op.getAction() instanceof Insert){
    			isFromSource=false;	
    		}    		
    		
    		writeLog(LogLevel.finer,"--- srcNode ---");
    		writeLog(LogLevel.finer,srcNode);
    		if (srcNode.getPosition() != null && !(srcNode.getPosition() instanceof NoSourcePosition)){    			
    			writeLog(LogLevel.finer,"srcPos: "+srcNode.getPosition().getLine() +" to " +srcNode.getPosition().getEndLine());    			
    			//findLine
    		}else
    			writeLog(LogLevel.finer,"no src position");
    		//get method context
    		writeLog(LogLevel.finer,"srcPath: \n"+srcNode.getPath());
    		
    		CtElement methodSrc = getContainCodeSnippet(srcNode);
    		    		
    		gumtree.spoon.diff.support.SpoonSupport support = new gumtree.spoon.diff.support.SpoonSupport();
    		CtElement methodTgt = support.getMappedElement(diff, methodSrc, isFromSource);
    		CtElement methodTgt2 = support.getMappedElement(diff, methodSrc, !isFromSource);
    		
    		if (!isFromSource){
    			CtElement tmp = methodSrc;
    			methodSrc=methodTgt;
    			methodTgt = tmp;
    		}
    		Pair<CtElement, CtElement> pair = new Pair<CtElement, CtElement>(methodSrc, methodTgt);    		
			writeLog(LogLevel.finer,"--- source Method: ---");
			writeLog(LogLevel.finer,methodSrc);
			writeLog(LogLevel.finer,"--- target Method: ---");
			writeLog(LogLevel.finer,methodTgt);
			
			if (pair.first==null || pair.second==null){
				writeLog(LogLevel.warning,"WARNING: null source or dest method");
				continue;
			}
			
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
    	if (diffPairs.size()>0) {
    		writeLog(LogLevel.info,bugFilePath);
        	writeLog(LogLevel.info,patchFilePath);
        	writeLog(LogLevel.info,"DiffPairs size: "+diffPairs.size());
    		//writeLog(LogLevel.info, "DiffPairs size: "+diffPairs.size()+"; WriteTo: "+outputDir);
    	}
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
    		String origCode = pair.first.getOriginalSourceFragment().getSourceCode();  		
    		try{
	    		FileUtils.writeToFile(originCodeSnippetFile,origCode);
    		}catch (IOException ex){
    			System.err.println("Error while writing origin code snippet "+i+": "+ ex.getMessage());
    			System.err.println(origCode);
    			writeLog(LogLevel.error, "Error while writing origin code snippet "+i+": "+ ex.getMessage());
    		}
    		
    		String patchedCode = pair.second.getOriginalSourceFragment().getSourceCode();
    		try{
    			FileUtils.writeToFile(patchedCodeSnippetFile,patchedCode);
    		}catch (IOException ex){
    			System.err.println("Error while writing patched code snippet "+i+": "+ ex.getMessage());
    			System.err.println(patchedCode);
    			writeLog(LogLevel.error,"Error while writing patched code snippet "+i+": "+ ex.getMessage());
    		}
    	}
	}
	
	static void writeLog(LogLevel level,Object msg) {
		if (logLevel.ordinal() >= level.ordinal())
			System.out.println(msg);
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
