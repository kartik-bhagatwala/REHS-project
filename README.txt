This is an IntelliJ project, Main class is located in /src/com.company
I used multiple Maven libraries, the libraries are as follows:

for writing to excel spreadsheet:
org.apache.poi:poi-ooxml:3.16 

for reading the json:
com.googlecode.json-simple:json-simple:1.1.1

You will also need Stanford corenlp-full, found at 
https://stanfordnlp.github.io/CoreNLP/download.html

Place your .xslx file in the same directory as the program, rename it to ids.xlsx, a sample xslx file is included in this directory, which demonstrates the format that ids can be read and analyzed.