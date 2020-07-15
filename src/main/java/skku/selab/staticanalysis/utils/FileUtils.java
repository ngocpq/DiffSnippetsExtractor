package skku.selab.staticanalysis.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

public class FileUtils {

	public static void writeToFile(String filePath,String content) throws IOException {		
		File file = new File(filePath);
		//String dirPath = filePath.substring(0, filePath.lastIndexOf(File.separator));
		String dirPath = file.getParent();
		File dir = new File(dirPath);
		if (!dir.exists()){
			dir.mkdirs();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
	    writer.write(content);
	    writer.close();
	}

	public static String[] getAllFilesInFolder(String patchDirPath,String regexString) {
		File dir = new File(patchDirPath);
		//String regexString = ".[jJ]ava$";
		RegexFileFilter regex = new RegexFileFilter(regexString); //"^(.*?)"
		Collection<File> list = org.apache.commons.io.FileUtils.listFiles(dir, regex, DirectoryFileFilter.DIRECTORY);
		String[] rs = new String[list.size()];
		int i=0;
		for(File file:list){
			String absPath = file.getAbsolutePath();
			int len = dir.getAbsolutePath().length();
			String relatePath = absPath.substring(len+1);
			rs[i] = relatePath;
			i++;
		}
		return rs;
	}

}
