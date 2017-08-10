## Introduction

This is an IntelliJ project to find dates, Main class is located in /src/com.company
I used multiple Maven libraries, the libraries are as follows:

for writing to excel spreadsheet:
org.apache.poi:poi-ooxml:3.16 

for reading the json:
com.googlecode.json-simple:json-simple:1.1.1

You will also need Stanford corenlp-full, found at 
https://stanfordnlp.github.io/CoreNLP/download.html

## Technical

Place your .xslx file in the same directory as the program, rename it to ids.xlsx, a sample xslx file is included in this directory, which demonstrates the format that ids can be read and analyzed. 

There are three text files that are used. stopwords.txt are words that can cause the program to mess up, such as current, yr, or quarter. These words are automatically removed from any string that is analyzed by the code. The other two are used to classify entries as either events or ranges, add words to these files to change the words that are used to classify entries.