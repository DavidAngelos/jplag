## Run JPlag

Type `java -jar jplag-yourVersion.jar` in a console to see the command line options.
The options are:

```
JPlag (Version 2.15.4-SNAPSHOT), Copyright (c) 2004-2017 KIT - IPD Tichy, Guido Malpohl, and others.
Usage: JPlag [ options ] <root-dir> [-c file1 file2 ...]
 <root-dir>      The root-directory that contains all submissions

options are:
 -v[qlpd]        (Verbose)
                 q: (Quiet) no output
                 l: (Long) detailed output
                 p: print all (p)arser messages
                 d: print (d)etails about each submission
 -d              (Debug) parser. Non-parsable files will be stored.
 -S <dir>        Look in directories <root-dir>/*/<dir> for programs.
                 (default: <root-dir>/*)
 -s              (Subdirs) Look at files in subdirs too (default: deactivated)

 -p <suffixes>   <suffixes> is a comma-separated list of all filename suffixes
                 that are included. ("-p ?" for defaults)

 -o <file>       (Output) The Parserlog will be saved to <file>
 -x <file>       (eXclude) All files named in <file> will be ignored
 -t <n>          (Token) Tune the sensitivity of the comparison. A smaller
                 <n> increases the sensitivity.
 -m <n>          (Matches) Number of matches that will be saved (default:20)
 -m <p>%         All matches with more than <p>% similarity will be saved.
 -r <dir>        (Result) Name of directory in which the web pages will be
                 stored (default: result)
 -a <dir>        (Archival) Directory containing archived submissions, which
                 will be only compared against
 -bc <dir>       Directory which contains the basecode (common framework)
 -c [files]      Compare a list of files. Should be the last one.
 -l <language>   (Language) Supported Languages:
                 java113 (default), java17, java15, java15dm, java12, java11, python3, c/c++, c#-1.2, char, text, scheme, scala, json, php, javascript, jupyter, r
```

**Note:** java19 refers to all java version from 9 on (currently 9 - 12).

### Example
Assume that we want to check students' solutions that are written in Java 11.

Each student solution is in its own directory, say `student1`, `student2`, and so on.
All solutions are in a common directory, say `exercise1`.

To run JPlag, simply type `java -jar jplag-yourVersion.jar -l java19 -r /tmp/jplag_results_exercise1/ -s /path/to/exercise1`

- `-l java19` tells JPlag to use the frontend for Java 9+
- `-s` tells JPlag to recurse into subdirectories; as we assume Java projects, we'll very likely encounter subdirectories such as `student1/src/`
- `-r /tmp/jplag_results_exercise1` tells JPlag to store the results in the directory `/tmp/jplag_results_exercise1`

**Note:** You have to specify the language exactly as they are printed by JPlag (running JPlag without command line arguments prints all available languages - and other options).
E.g., if you want to process C++ files, you have specify `-l c/c++` as language option.

### Options
#### `-x <file>`   (eXclude) All files named in `<file>` will be ignored
The option `-x` requires an exclusion list saved as `<file>`.
The exclusion list contains a  number of suffixes.
JPlag will ignore all files that end with one of the suffixes.

#### `-c [files]`   (Compare) Compare a list of files
Example: `java -jar jplag-yourVersion.jar -l java19 -c student1_file student2_file student3_file`
This option must be the last one.
JPlag will compare just a list of files pairwise.

#### `-bc <dir>`   (common framework) Name of the directory which contains the basecode
Example: `java -jar jplag-yourVersion.jar -s -l java19  ./submissions -bc template`
This option includes files that were given out to students as a framework or to fill in blanks - the content is compared with each submission and matching parts are excluded from mutual student matching.
`<dir>` is considered to be the name of a subdirectory, i.e. relative path from `<root-dir>`, residing somewhere in the submission directory, on the same level as student submissions.
**Note:** Due to a bug in all versions you have to provide the base directory without a slash at the end (e.g template, **not** template/).

## Building JPlag
To build and run a local installation of JPlag, you can use the pom.xml in this directory (aggregator). It builds JPlag and the available frontends.

To generate a single JAR file run `mvn clean generate-sources assembly:assembly` inside the `jplag` directory. You will find the JAR in the `jplag/target` directory.

## Javascript Support
In order to run JPlag with Javascript support you will need to download [jplag-javascript-parser](https://github.com/DavidAngelos/jplag-javascript-parser).

In the root directory of the `jplag-javascript-parser` copy the JAR file generated from the previous step.

The `jplag-javascript-parser` folder structure should be the following:

    jplag-javascript-parser/
    ├── index.js                   
    ├── jplag-2.15.4-SNAPSHOT-jar-with-dependencies.jar                    
    ├── LICENSE                     
    ├── package.json                    
    ├── package-lock.json
    └── run.sh

Now run `java -jar jplag-2.15.4-SNAPSHOT-jar-with-dependencies.jar /path/to/project -r /path/to/results -l javascript`

### Adding new languages
Adding a new language frontend is quite simple. Have a look at one of the `jplag.frontend` projects. All you need is a parser for the language (e.g., for ANTLR or for JavaCC) and a few lines of code that sends the tokens (that are generated by the parser) to JPlag.
