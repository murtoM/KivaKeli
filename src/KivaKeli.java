// SPDX-FileCopyrightText: 2021 Markus Murto
//
// SPDX-License-Identifier: Apache-2.0

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Scanner;
import java.io.PrintWriter;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

/**
 * Print current temperature and weather condition in Finnish with UNIX-like 
 * command-line options.
 *
 * Takes POSIX-standard command-line flags to modify settings in runtime:
 * -h                   help message
 * -l <location name>   location, eg. "Turku"
 * -u <symbol>          temperature unit symbol, eg. C
 *
 *  OpenWeatherMap API key is stored locally in file api_key.txt in the 
 *  current working directory.
 *
 * @author  Markus Murto
 */
public class KivaKeli {
  // initialize user input reader
  private static final Scanner inputReader = new Scanner(System.in);

  // help message for the user in Finnish
  private static final String helpMessage = "Käyttö: KivaKeli " + 
    "[komentorivioptiot...]\n\n" +
    "-h\t\t" +
    "tulostetaan tämä apuviesti\n" +
    "-l <sijainti>\t" +
    "aseta sijainti, esimerkiksi kaupungin nimi\n" +
    "-u <symboli>\t" +
    "aseta haluttu lämpötilan symboli, esimerkisi C tai F\n";

  // set initial application settings
  // will the application fetch a forecast (or just the current weather)
  //private static boolean isForecast = false;
  // weather location name, eg. "Turku""
  private static String locName = "";
  // temperature unit symbol
  private static char tempUnit = 'K';

  /**
   * Fetches OWM API key, parses command-line options and then prints weather
   * information for the user.
   *
   * Takes POSIX standard command-line flags to modify settings in runtime.
   *
   * The OpenWeatherMap API key is stored locally in file api_key.txt in the
   * current working directory. API key is interactively asked, if the file is
   * not in the working directory.
   *
   * @param   args  command-line arguments, POSIX standard flags
   */
  public static void main(String[] args) {
    try {
      String apiKey = keyFileManager();
      if (args.length == 0) {
        printWeather(apiKey);
      } else {
        try {
          argParser(args);
        }
        catch(ArrayIndexOutOfBoundsException e) {
          System.out.println("Virheellinen argumentti!");
          System.exit(1);
        }
        printWeather(apiKey);
      }
    }
    catch(MalformedURLException e) {
      System.out.println("Virhellinen URL: " + e);
      System.exit(1);
    }
    catch(ParseException e) {
      System.out.println("Virhellinen dokumentti: " + e);
      System.exit(1);
    }
  }

  /**
   * Simple way to parse POSIX-standard command-line arguments.
   *
   * Sets application settings according to the flags provided by the user:
   * - location
   * - temperature unit
   * - prints help message
   *
   * @param   args  raw command-line arguments
   */
  public static void argParser(String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch(args[i]) {
        case "-h":
          System.out.print(helpMessage);
          System.exit(0);
        case "-l":
          locName = args[i+1];
          i++;
          break;
        case "-u":
          tempUnit = args[i+1].charAt(0);
          i++;
          break;
        default:
          System.out.println("Virheellinen argumentti: " + args[i]);
          System.exit(1);
      }
    }
  }

  /**
   * Asks user for OpenWeatherMap API-key and returns it as a String.
   *
   * @return    OpenWeatherMap API-key
   */
  public static String askApiKey() {
    System.out.print("Syötä API-avain: ");
    String key = inputReader.nextLine();
    return key;
  }


  /**
   * Builds the OpenWeatherMap API URL, from city name, api key and temperature unit 
   * parametres and returns the URL.
   *
   * @param   loc     city name and optionally country code
   * @param   apiKey  OpenWeatherMap api key
   * @param   unit    wanted temperature unit, 'C' or 'F' expected, if parametre 
   *                  is empty or invalid, Kelvin units are used instead
   * @return          full API url
   */
  public static String buildCityUrl(String loc, String apiKey, char unit) {
    String url = "http://api.openweathermap.org/data/2.5/weather?";
    url += "q=" + loc + "&appid=" + apiKey + "&units=" + getUnitKeyword(unit) + "&lang=fi";
    return url;
  }

  /**
   * Builds the OpenWeatherMap API URL, from geographical coordinates, the API KEY 
   * and temperature unit.
   *
   * @param   lat     latitude coordinate
   * @param   lon     longitude coordinate
   * @param   apiKey  OpenWeatherMap API key
   * @param   unit    temperature symbol, 'C' or 'F' expected, but if parametre
   *                  is empty or invalid, Kelvin units are used instead
   * @return          full API URL
   */
  public static String buildGeoUrl(String lat, String lon, String apiKey, char unit) {
    String url = "http://api.openweathermap.org/data/2.5/weather?";
    url += "lat=" + lat + "&lon=" + lon + "&appid=" + apiKey + "&units=" + getUnitKeyword(unit) + "&lang=fi";
    return url;
  }

  /**
   * Convert character temperature unit symbol to API expected keyword.
   *
   * If the unit symbol is not 'C' or 'F', continue with kelvin "standard".
   *
   * @param   unit  Temperature unit symbol, 'C' or 'F' expected
   * @return        api expected keyword ("metric", "imperial", or "standard")
   */
  public static String getUnitKeyword(char unit) {
    String apiUnit = "";
    switch(unit) {
      case 'c':
        apiUnit = "metric";
        tempUnit = 'C';
        break;
      case 'C':
        apiUnit = "metric";
        break;
      case 'F':
        apiUnit = "imperial";
        break;
      case 'f':
        apiUnit = "imperial";
        tempUnit = 'F';
        break;
      default:
        apiUnit = "standard";
        tempUnit = 'K';
    }
    return apiUnit;
  }

  /**
   * Returns the http GET response content from the url given as a parametre.
   *
   * @param   inputUrl  url used to fetch GET response
   * @return            GET response
   */
  public static String httpRequest(String inputUrl) throws MalformedURLException {
    // establish a http connection and set the request parametres
    String responseContent = "";
    boolean unauthorizedRequest = false;
    try {
    URL url = new URL (inputUrl);
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Accept", "application/json");

    // invalid api key
    if (conn.getResponseCode() == 401) {
      unauthorizedRequest = true;
    }

    // uncomment to print reponse message DEBUG
    //System.out.println("DEBUG: " + conn.getResponseCode() + " " + conn.getResponseMessage());

    // setup to read the response content
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String inputLine;
    // read GET response to StringBuffer
    StringBuffer response = new StringBuffer();
    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    // close BufferedReader
    in.close();
    // convert StringBuffer to vanilla String
    responseContent = response.toString();
    // close http connection
    conn.disconnect();
    }

    // error in request, possibly invalid API key or city name
    catch(IOException e) {
      System.out.println("Virhe: " + e);
      if (unauthorizedRequest) {
        System.out.println("Syötitkö virheellisen API-avaimen?");
      } else {
        System.out.println("Syötitkö virheellisen sijainnin?");
      }
      System.exit(1);
    }
    return responseContent;
  }

  /**
   * Increase the size of provided String array by one.
   *
   * The content of the array will be copied to the new array with increased 
   * size.
   *
   * @param   org   original array to be incremented
   * @return        new array with larger size
   */
  public static String[] incrementStringArray(String[] org) {
    String[] inc = new String[org.length + 1];

    for (int i = 0; i < org.length; i++) {
      inc[i] = org[i];
    }
    return inc;
  }

  /**
   * Fetches geographical coordinates (latitude and longitude) from ip-api 
   * using the current user IP address.
   *
   * The coordinates are stored in String array, where the first value is the
   * latitude coordinate and the second value is the longitude coordinate.
   *
   * @return    user latitude and longitude coordinates in String array
   */
  public static String[] ipGeoLoc() throws ParseException, MalformedURLException {
    String[] latLon = new String[2];

    String rawLoc = httpRequest("http://ip-api.com/json/?fields=lat,lon");
    latLon[0] = parseField(rawLoc, "lat");
    latLon[1] = parseField(rawLoc, "lon");
    return latLon;
  }

  /**
   * Checks if the "api_key.txt" exists - if not, ask the user for the key and 
   * write it, and then the method returns the API-key as a String.
   *
   * If the "api_key.txt" file does exist, the API-key is read from the file. 
   * If the file does not exist, user is asked to input the API-key, which is
   * then written to the file.
   *
   * @return    API-key from the file or user input
   */
  public static String keyFileManager() {
    File keyFile = new File("api_key.txt");
    String apiKey = "";
    // create a new file, if it doesn't exists and ask user for the API-key
    // if the file does exist, read the API-key from the file
    try {
      if (keyFile.createNewFile()) { // file not found, create a new one
        System.out.println("API-avainta ei löytynyt!");
        PrintWriter writer = new PrintWriter(keyFile);
        System.out.print("Syötä API-avain: ");
        apiKey = inputReader.nextLine();
        writer.println(apiKey);
        writer.close();
      } else { // file was found
        Scanner fileReader = new Scanner(keyFile);
        apiKey = fileReader.nextLine();
        fileReader.close();
      }
    }
    catch(IOException e) {
      System.out.println("Virhe: " + e);
      System.exit(1);
    }
    return apiKey;
  }

  /**
   * Convenience method, parses JSON document for the matching field.
   *
   * Parses the first found field matching the field String from the json 
   * String and returns the field's value as a String.
   *
   * for example:
   *  "field": "returned value"
   *
   * @param   json    JSON document to be parsed
   * @param   field   name of the field on the document to be parsed
   * @return          value from the field in the document
   */
  public static String parseField(String json, String field) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject jsonField = (JSONObject) parser.parse(json);
    return (String) jsonField.get(field).toString();
  }

  /**
   * Convenience method, parses JSON document for mathing fields in matching
   * JSON array.
   *
   * Parses the first found JSON array from the document matching the field 
   * String. From this array parses the the matching fields and returns the 
   * fields' values as a String array.
   *
   * for example:
   *  "field": [
   *    "finalField": "returned value"
   *  ]
   *
   * @param   json        JSON document to be parsed
   * @param   field       name of the array in the document to be parsed
   * @param   finalField  name of the field in the array to be parsed
   * @return              value from the field in the array
   */
  public static String[] parseArray(String json, String field, String finalField) throws ParseException {
    String[] returnFields = new String[1];;
    // find the array mathing **field**
    JSONParser parser = new JSONParser();
    JSONObject jsonField = (JSONObject) parser.parse(json);
    JSONArray fieldContent = (JSONArray) jsonField.get(field);

    // find the **finalField** in the array
    Iterator i = fieldContent.iterator();
    int cnt = 0;
    while (i.hasNext()) {
      JSONObject nextField = (JSONObject) i.next();
      returnFields[0] = (String)nextField.get(finalField);
      returnFields = incrementStringArray(returnFields);
    }
    return returnFields;
  }

  /**
   * Prints current weather based on current device network location or city name.
   *
   * Possible city name or temperature unit is referenced and not given as 
   * parametre.
   *
   * @param   apiKey  OpenWeatherMap API key
   * @param   unit    temperature unit symbol
   */
  public static void printWeather(String apiKey) throws ParseException, MalformedURLException {
    String[] geoLoc = ipGeoLoc();
    String url = "";
    if (locName.equals("")) {
      System.out.println("Sää laitteen sijainnissa\n");
      url = buildGeoUrl(geoLoc[0], geoLoc[1], apiKey, tempUnit);
    } else {
      System.out.println("Sää " + locName + "\n");
      url = buildCityUrl(locName, apiKey, tempUnit);
    }
    String rawJson = httpRequest(url);
    String mainSubDoc = parseField(rawJson, "main");
    if (tempUnit == 'K') {
      System.out.println("Lämpötila: " + parseField(mainSubDoc, "temp") + " K");
    } else {
      System.out.println("Lämpötila: " + parseField(mainSubDoc, "temp") + " °" + tempUnit);
    }
    String weather = parseArray(rawJson, "weather", "description")[0];
    System.out.println("Keli: " + weather);
  }
}
