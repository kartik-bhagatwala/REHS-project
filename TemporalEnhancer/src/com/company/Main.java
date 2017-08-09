package com.company;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.time.SUTime;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.time.TimeAnnotator;
import edu.stanford.nlp.time.TimeExpression;
import edu.stanford.nlp.util.CoreMap;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Main {


    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        System.out.println(System.getProperty("user.dir"));
        Properties props = new Properties();
        ArrayList<String> readexcel=new <String>ArrayList();
        ArrayList <String[]> finaldates=new <String[]>ArrayList();
        readexcel=readexcel(0); //forms an arraylist of the first column of the excel spreadsheet

        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse,");//setting properties for the annotator
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        pipeline.addAnnotator(new TimeAnnotator("sutime",props));
        for(int i=1;i<readexcel.size();i++){
            finaldates.add(findbyid(readexcel.get(i),pipeline));
        }
        //writes the final values that we get to excel
        for(int p=0;p<finaldates.size();p++){
            for(int x=0;x<finaldates.get(p).length;x++){
                if(x==0){
                    //System.out.println("date "+p+" timeperiod_begin "+finaldates.get(p)[x]);
                    writeexcel(finaldates.get(p)[x],2,p+2);//use plus 2 because first row is the descriptors
                    writeexcel(categories.get(p),6,p+2);
                    writeexcel(Integer.toString(timerefs.get(p)),7,p+2);
                }
                if(x==1){
                    //System.out.println("date "+p+" timeperiod_end "+finaldates.get(p)[x]);
                    writeexcel(finaldates.get(p)[x],3,p+2);
                }
            }
        }
        long endTime=System.currentTimeMillis();
        System.out.println("That took "+(endTime-startTime)+" milliseconds, or "+(endTime-startTime)/1000+" seconds");


    }
    private static ArrayList<String> categories=new<String>ArrayList();
    private static ArrayList<Integer> timerefs=new ArrayList<Integer>();
    private static Boolean hasday(String date){
        return date.length() > 7;
    }
    private static Boolean hasmonth(String date){
        return date.length() >= 5;
    }
    private static Boolean hasyear(String date){
        return !date.substring(0, 4).contains("X");
    }
    private static ArrayList<String> readexcel(int col) throws IOException {
        String strFile=System.getProperty("user.dir")+"\\ids.xlsx"; //IDS THAT YOU WANT TO ANALYZE IN .xlsx format
        InputStream inp = new FileInputStream(strFile);
        XSSFWorkbook wb = new XSSFWorkbook(inp);
        ArrayList<String> excelids=new ArrayList<String>();
        Sheet sheet1 = wb.getSheetAt(col);
        for (Row row : sheet1) {
            Cell c = row.getCell(0);
            if(c != null) {
                CellReference cellRef = new CellReference(row.getRowNum(), c.getColumnIndex());
                System.out.print(cellRef.formatAsString());
                System.out.print(" - ");
                System.out.println(c.getRichStringCellValue().getString());
                excelids.add(c.getRichStringCellValue().getString());

            }
        }
        inp.close();
        return excelids;

    }
    private static void writeexcel(String towrite, int col, int rowp) throws IOException, InvalidFormatException {
        String strFile=System.getProperty("user.dir")+"\\ids.xlsx";
        InputStream inp=new FileInputStream(strFile);
        XSSFWorkbook wb=new XSSFWorkbook(inp);

        Sheet sheet1=wb.getSheetAt(0);
        Row r=sheet1.getRow(rowp-1); // cuz the rows and columns are based off 0 scales
        if (r==null){
            // First cell in the row, create
            r=sheet1.createRow(rowp-1);
        }
        Cell c=r.getCell(col-1); // 4-1
        if (c==null) {
            // New cell
            c=r.createCell(col-1, CellType.STRING);
        }
        c.setCellValue(towrite);
        System.out.println("writing "+towrite+" to column "+col);
        FileOutputStream fileOut = new FileOutputStream(strFile);
        wb.write(fileOut);
        fileOut.close();
        inp.close();


    }
    private static String[] findbyid(String id, StanfordCoreNLP pipeline) throws Exception {
        System.out.println("the id is:"+id);
        String jsontxt=readUrl("http://132.249.238.169:8080/geoportal/rest/metadata/item/"+id+"?pretty=true");
        String []arr=new String[2];

        ArrayList getdates;
        Object obj= JSONValue.parse(jsontxt);
        JSONObject jsonObject=(JSONObject) obj;
        JSONObject src= (JSONObject) jsonObject.get("_source");
        JSONObject timeperiod=(JSONObject) src.get("timeperiod_nst");
        if (timeperiod!=null){
            String begin=(String) timeperiod.get("begin_dt");
            System.out.println("timeperiod from the json is "+begin);
            String abstracttxt=(String)src.get("title")+"  "+ src.get("apiso_Abstract_txt");
            System.out.println(abstracttxt);
            if(src.get("apiso_Abstract_txt")==null || src.get("apiso_Abstract_txt").equals("Legacy product - no abstract available")){
                abstracttxt=(String) src.get("title")+"  "+src.get("description");
                System.out.println("looking at description for the date");
            }
            getdates=getdates(abstracttxt,pipeline);
            System.out.println("--");

        }else{
            //this runs if there is nothing in  timeperiod_nst
            System.out.println("no temporal extent!");
            String abstracttxt=(String)src.get("title")+"  "+ src.get("apiso_Abstract_txt");
            System.out.println(abstracttxt);
            if(abstracttxt==null|| src.get("apiso_Abstract_txt").equals("Legacy product - no abstract available")){
                abstracttxt=(String) src.get("title")+"  "+src.get("description");
                System.out.println("looking at description for the dates");
            }
            getdates = getdates(abstracttxt, pipeline);
            System.out.println("--");

        }
        if(getdates.size()!=0){
            arr[0]=((String[])getdates.get(0))[0];
            arr[1]=((String[])getdates.get(1))[0];
        }

        return arr;

    }
    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }
    private static ArrayList getdates(String txt,StanfordCoreNLP pipeline){
        //start of the word analyzing part
        String text = txt;
        System.out.println("before trimming:"+text);
        // create an empty Annotation just with the given text

        ArrayList <Integer> dateranks= new <Integer>ArrayList(); //3 is only year found, 4 is only year+month, and 5 is a full date

        Boolean wordafterdate=false;
        Boolean lookingformonth=true;
        int timeref=0;
        Boolean saveword=false;
        String rangedate="";
        String monthsave="";
        String category=null;
        Annotation document = new Annotation(text);
        // run all Annotators on this text
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        StringBuilder datestring= new StringBuilder(" ");
        for(CoreMap sentence: sentences) {
            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                // this is the text of the token
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                // this is the POS tag of the token
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                System.out.println("word: " + word + " pos: " + pos + " type:" + ne);
                if(category==null){
                    category=classify(word);
                }
                if(saveword &&  (! word.matches(".*[a-z].*") )){
                    rangedate=word;
                    System.out.println("saving this daterange: "+word);
                    saveword=false;
                    lookingformonth=false;


                }
                if(wordafterdate && word.equals("-") ){
                    wordafterdate=false;
                    saveword=true;
                }

                if(!ne.equals("DATE")){
                    //txt=txt.replaceFirst("\\b"+java.util.regex.Pattern.quote(word)+"\\b"," ");//regexs are weird
                    //txt=txt.replaceFirst(java.util.regex.Pattern.quote(word)," ");//regexs are weird
                    wordafterdate=false;

                    System.out.println("replacing this word:"+word);
                    datestring.append(" ");

                }else {
                    if(!word.matches(".*[0-9].*") && lookingformonth){
                        monthsave=word;
                        System.out.println("month recognized: "+monthsave+rangedate);
                    }

                    datestring.append(" ").append(word);
                    wordafterdate=true;
                    saveword=false;


                }
            }
            //txt = txt.replaceAll("[()]", "");
            datestring= new StringBuilder((datestring.toString()));
        }
        datestring.append(monthsave).append(" ").append(rangedate);
        txt = txt.replaceAll("\\.", " ");
        datestring = new StringBuilder(replace(datestring.toString()));
        //end of the words trimming part
        System.out.println("datestring:"+datestring);
        //System.out.println("txt:"+txt);
        String keepyear=""; //this is a year for those odd situations where a year is mentioned earlier, but then later refernces to the date do not contain the year
        String keepmonth="";
        Boolean addedmonth=false,addedday=false;
        ArrayList <String>dates= new <String>ArrayList();
        Annotation document1 = new Annotation(datestring.toString());
        // run all Annotators on this text
        pipeline.annotate(document1);
        Integer rank=5;

        System.out.println(document1.get(CoreAnnotations.TextAnnotation.class));
        List<CoreMap> timexAnnsAll=document1.get(TimeAnnotations.TimexAnnotations.class);
        for(CoreMap cm:timexAnnsAll){
            List<CoreLabel> tokens=cm.get(CoreAnnotations.TokensAnnotation.class);
            System.out.println(cm + " [from char offset " +
                    tokens.get(0).get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) +
                    " to " + tokens.get(tokens.size() - 1).get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + ']' +
                    " --> " + cm.get(TimeExpression.Annotation.class).getTemporal());

            timeref++;
            SUTime.Temporal tim=cm.get(TimeExpression.Annotation.class).getTemporal();
            String time=tim.toISOString();
            System.out.println("we get this string:"+time);
            rank=5;
            addedday=false;
            addedmonth=false;
            if (time!=null){
                if(!time.substring(0, 4).contains("X")){
                    keepyear=time.substring(0,4);
                    System.out.println("saved the year of "+keepyear);

                }
                if(hasmonth(time)){
                    keepmonth=time.substring(5,7);
                    System.out.println("saved month of "+keepmonth);

                }
                if(!hasyear(time)){
                    if(keepyear.equals("")){
                        System.out.println("searching whole sentence for year");
                        for(CoreMap c:timexAnnsAll){
                            SUTime.Temporal findyear=c.get(TimeExpression.Annotation.class).getTemporal();
                            String findyears=findyear.toISOString();
                            if(!findyears.substring(0, 4).contains("X")){
                                keepyear=findyears.substring(0,4);
                                System.out.println("saved the year of(findyears func) "+keepyear);
                                break;
                            }
                        }
                    }
                    time=time.substring(4);//cuts out year(XXXX) if there is none
                    time=keepyear+time;
                    System.out.println("using saved year");

                }

                if(!hasmonth(time)){
                    time=time+"-01";//adds a start month of january if we only get a year
                    addedmonth=true;
                }
                if(!hasday(time)){
                    time=time+"-01"; //adds a start day of 1 if we only get a month
                    addedday=true;
                }
                if(addedday){
                    rank=3;
                    System.out.println("Setting rank of 3");
                }
                if(addedmonth){
                    rank=2;
                }
                try{
                    LocalDate d=LocalDate.parse(time);
                    System.out.println(d);
                    LocalDateTime dt=d.atStartOfDay();
                    String begintime=dt.toString();
                    begintime=begintime+":00Z";
                    System.out.println(begintime);
                    dates.add(begintime);
                    dateranks.add(rank);


                }catch (Exception e){
                    System.out.println("got an exception of "+e);
                }
                System.out.println("removing this because already analyzed:"+cm);
                datestring = new StringBuilder(datestring.toString().replaceFirst("\\b" + cm + "\\b", " "));

            }

        }

        System.out.println(dateranks);
        System.out.println(dates);
        System.out.println("after everyting we got this:"+datestring);
        if(dates.size()>1){
            if(dates.get(0).equals(dates.get(1))){
                dates.remove(1);
                dateranks.remove(1);
            }
        }

        if(dates.size()>=2){
            System.out.println("has two dates");
        }else if(dates.size()==1){//if only one date or month is extracted, we make two dates/times
            if(addedmonth){
                String year= dates.get(0);
                dates.add(1,year.substring(0,4)+"-12-31T00:00:00Z");
                rank=2;
                dateranks.add(rank);
            }else if(addedday){
                String dateString = dates.get(0);
                dateString=dateString.substring(0,10);
                LocalDate date = LocalDate.parse(dateString);
                LocalDate newDate=null;
                if(date.getMonth().toString().equals("FEBRUARY")){//if month is february, dont use leap year days becuase it causes problems
                    System.out.println("no leap year");
                    newDate = date.withDayOfMonth(date.getMonth().maxLength()-1);

                }else {
                    newDate = date.withDayOfMonth(date.getMonth().maxLength());//finds last day of the month to create a begin and end date

                }
                System.out.println("got one date, using last day of month");
                dates.add(1,newDate+"T00:00:00Z");//adding last date of month to arraylist
                rank=3;
                dateranks.add(rank);

            }
        }else {
            System.out.println("something went very wrong.");//program should never get here
        }
        ArrayList datesplusranks=new <String>ArrayList();
        for(int i=0;i<dates.size();i++ ){
            String[] arr = new String[2];
            arr[0]=dates.get(i);
            arr[1]= String.valueOf(dateranks.get(i));

            datesplusranks.add(i,arr);
            //datesplusranks.add(arr);
            System.out.println("adding "+dates.get(i)+" at index "+i);
            //String[]arrayc=new String[2];
            //arrayc= (String[]) datesplusranks.get(i);
            //System.out.println("rank adding "+ arrayc[1]);


        }
        //insertion sort
        for (int i=1; i<datesplusranks.size(); ++i)
        {
            int key = Integer.parseInt(((String[])datesplusranks.get(i))[1]);
            int j;
            for(j=i-1;j>=0 && key>Integer.parseInt(((String[])datesplusranks.get(j))[1]); j--){
                Collections.swap(datesplusranks,j+1,j);
            }
            System.out.println("index "+i+" is "+Integer.parseInt(((String[])datesplusranks.get(i))[1]));
        }
        int stopval=2;
        Integer keyval=findkeyval(datesplusranks);

        if(datesplusranks.size()>2){
            for(int i=1;i<datesplusranks.size();i++){

                if (Integer.parseInt(((String[])datesplusranks.get(i))[1])<keyval){
                    stopval=i-1;
                    break;
                }
            }
            for (int i=1; i<stopval; ++i) {
                int j;
                String[]arr=(String[])datesplusranks.get(i);
                LocalDate d1=LocalDate.parse(arr[0].substring(0,10));
                for(j=i-1;j>=0 && d1.isBefore(LocalDate.parse(((String[])datesplusranks.get(j))[0].substring(0,10))); j--){
                    Collections.swap(datesplusranks,j+1,j);
                }
                System.out.println("index "+i+" is "+Integer.parseInt(((String[])datesplusranks.get(i))[1]));
            }
            for (int x=1; x<4;  x++){
                try{
                    if((((String[])datesplusranks.get(0))[0]).equals((((String[])datesplusranks.get(1))[0]))){
                        datesplusranks.remove(1);
                        System.out.println("removing dupes");
                    }
                }catch (Exception e){
                    System.out.println("error of" + e);
                }
            }

        }

        // removes duplicates, just in case
        Set<String> hs = new LinkedHashSet<>();
        hs.addAll(datesplusranks);
        datesplusranks.clear();
        datesplusranks.addAll(hs);

        if (datesplusranks.size()<=0){
            System.out.println("no date found!");

        }else{
            if (datesplusranks.size()==1){
                datesplusranks.add(1,datesplusranks.get(0));

            }
            if(Integer.parseInt(((String[])datesplusranks.get(0))[1])<=3 && Integer.parseInt(((String[])datesplusranks.get(1))[1])==5)  {
                datesplusranks.add(0,datesplusranks.get(1));

            }
            if(Integer.parseInt(((String[])datesplusranks.get(1))[1])<=3 && Integer.parseInt(((String[])datesplusranks.get(0))[1])==5){
                datesplusranks.add(1,datesplusranks.get(0));

            }


            String[]arr=(String[])datesplusranks.get(0);
            LocalDate d1=LocalDate.parse(arr[0].substring(0,10));
            arr=(String[])datesplusranks.get(1);
            LocalDate d2=LocalDate.parse(arr[0].substring(0,10));
            //checks that dates are in right order after removing duplicates, which can mess up order
            if(d1.isAfter(d2)){
                Collections.swap(datesplusranks,1,0);
                System.out.println("swapping");
            }
            System.out.println("start date: "+ ((String[])datesplusranks.get(0))[0]);
            System.out.println("end date: "+((String[])datesplusranks.get(1))[0]);
            System.out.println("dateranks:"+((String[])datesplusranks.get(0))[1]);
            System.out.println("dateranks1:"+((String[])datesplusranks.get(1))[1]);

        }
        categories.add(category);
        timerefs.add(timeref);
        return datesplusranks;
    }
    private static String replace(String txt){
        txt = txt.replaceAll("[ ]{3,}", " sp ");

        txt=txt.replaceAll("current","");
        txt=txt.replaceAll("previously","");
        txt=txt.replaceAll("month","");
        txt=txt.replaceAll("quarter","");

        return txt;
    }
    private static String getregexfromfile(String filename){//returns a regex of the words from a text file, stored in the working directory
        String regex;

        String regex="placeholder";
        try {
            input = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        input.useDelimiter(" +"); //delimiter is one or more spaces

        while(input.hasNext()){
            String word=input.next();
            System.out.println(word);
            regex=regex+"|"+word;
        }
        return regex;
    }
    private static Integer findkeyval(ArrayList datesplusranks){
        Integer key=0;
        for (Object datesplusrank : datesplusranks) {
            if (key < Integer.parseInt(((String[]) datesplusrank)[1])) {
                key = Integer.parseInt(((String[]) datesplusrank)[1]);
            }
        }
        return key;
    }

    private static String classify(String txt){
        String cat="";
        if(txt.equals("eruption") || txt.equals("earthquake") || txt.equals("hurricane")||txt.equals("storm")||txt.equals("eruption")||txt.equals("quake")){
            cat="event";
        }else if(txt.equalsIgnoreCase("survey") ||txt.equalsIgnoreCase("study") ||txt.equalsIgnoreCase("expedition")||txt.equalsIgnoreCase("research")||txt.equalsIgnoreCase("results")||txt.equalsIgnoreCase("summary")||txt.equalsIgnoreCase("investigations") ){
            cat="range";
        }else if(txt.equalsIgnoreCase("surveys")||txt.equalsIgnoreCase("conclusion") ||txt.equalsIgnoreCase("report")){
            cat="range";
        } else {
            cat=null;
        }


        return cat;
    }

}
