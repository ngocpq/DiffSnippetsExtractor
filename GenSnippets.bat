echo off

set jar_file=C:\workplace\staticanalysis\release\patchanalyser-0.0.1-SNAPSHOT-jar-with-dependencies.jar
set java_class=skku.selab.staticanalysis.DiffSnippetExtractor

set source_dir=C:\VirtualMachines\SharedFolder\quixbugs\buggy_programs
set output_dir="C:\VirtualMachines\SharedFolder\quixbugs\quixbugs_snippets"
set patchedPrograms_dir=C:\VirtualMachines\SharedFolder\quixbugs\patchedPrograms

for /d %%b in ("%patchedPrograms_dir%\*") do (
	echo %%~nb
	
	for /d %%t in ("%%b\*") do (  
		echo   %%~nt
		
		for /d %%p in ("%%t\*") do (
			echo     %%~np			
			rem echo     patch_dir: %%b\%%~nt\%%~np
			rem echo     source_dir: %source_dir%
			rem echo     output_dir: %output_dir%\%%~nb_%%~nt_%%~np
			
			echo processing: %%~nb\%%~nt\%%~np 
			echo java -cp %jar_file% %java_class% -s %source_dir% -p %%b\%%~nt\%%~np -o %output_dir%\%%~nb_%%~nt_%%~np			
			
			java -cp %jar_file% %java_class% -s %source_dir% -p %%b\%%~nt\%%~np -o %output_dir%\%%~nb_%%~nt_%%~np >gen_snippet_output_log.txt
		)					
	)
)


pause