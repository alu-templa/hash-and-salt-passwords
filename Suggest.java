import java.io.*;
import java.util.Scanner;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.SecureRandom;
import java.util.Random;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.xml.bind.DatatypeConverter;

public class Suggest {
    private static final Random RANDOM = new SecureRandom();
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    
    public static void main(String[] args) {
        queryForAccount();
    }

    private static String binaryToString(byte[] b){
        return DatatypeConverter.printBase64Binary(b);
    }

    private static byte[] stringToBinary(String str){
        return DatatypeConverter.parseBase64Binary(str);
    }

    private static byte[] getNextSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return salt;
    }

    private static byte[] hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e){
            throw  new AssertionError("Error while hashing a passsword: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean isExpectedPassword(char[] password, byte[] salt, byte[] expectedHash) {
        byte[] pwdHash, pwd;
        pwdHash = hash(password, salt);
        pwd = pwdHash;
        try{
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byteOut.write(pwdHash);
            byteOut.write(salt);
            pwd = byteOut.toByteArray();
        }catch(Exception e){
            e.printStackTrace();
        }
        Arrays.fill(password, Character.MIN_VALUE);
        if (pwd.length != expectedHash.length) return false;
            for (int i = 0; i < pwd.length; i++) {
        if (pwd[i] != expectedHash[i]) return false;
        }
        return true;
        
    }

    private static void queryForAccount(){
        Scanner in = new Scanner(System.in);
        System.out.print("Do you have an account? (y/n): ");
       
        String prevAccount = in.nextLine();

        if(prevAccount.equals("y")){
            loginOrSignup(true);
        } else if(prevAccount.equals("n")){
            loginOrSignup(false);
        } else {
            System.out.println("Invalid input");
            queryForAccount();            
        }
        in.close();
    }

    private static void loginOrSignup(boolean hasAccount){
        String[] ary = new String[2];
        Scanner in = new Scanner(System.in);
        String loginResult = "";

        if(hasAccount){
            System.out.print("Please enter your username: ");
            ary[0] = in.nextLine();
            System.out.print("Please enter your password: ");
            ary[1] = in.nextLine();
            loginResult = login(ary);
        } else {
            System.out.print("Please enter a username: ");
            ary[0] = in.nextLine();
            System.out.print("Please enter a password: ");
            ary[1] = in.nextLine();
            signup(ary);
        }
        in.close();
        System.out.println(loginResult);
    }

    private static String login(String ary[]){
        byte[] salt, sPwd;
        
        String usernameInput = ary[0];
        char[] passwordInput = new char[ary[1].length()];
        for(int i=0; i < ary[1].length(); i++){
            passwordInput[i] = ary[1].charAt(i);
        }
        
        try {
            Scanner in = new Scanner(new File("passwords.txt"));
            while (in.hasNextLine()){
                String s = in.nextLine();
                String[] sArray = s.split(",");
                
                sPwd = stringToBinary(sArray[1]);
                salt = stringToBinary(sArray[2]);

                if (usernameInput.equals(sArray[0]) && isExpectedPassword(passwordInput, salt, sPwd)){//passwordInput.equals(sArray[1])){
                    return ("Login Successful");
                }
            }
            in.close();
            return ("Login Failed");
        } catch (FileNotFoundException e) {
            return ("File Not Found");
        }
    }

    private static void signup(String ary[]){
        if(checkSignupInput(ary)){
            File inputFile = new File("passwords.txt");
            byte[] salt, hash, pwd;
            String passwordInput, saltStr;
            
            String usernameInput = ary[0];
            char[] ch = new char[ary[1].length()];
            for(int i=0; i < ary[1].length(); i++){
                ch[i] = ary[1].charAt(i);
            }
            salt = getNextSalt();
            hash = hash(ch, salt);
            
            
            try {
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                byteOut.write(hash);
                byteOut.write(salt);
                pwd = byteOut.toByteArray();

                passwordInput = binaryToString(pwd);
                saltStr = binaryToString(salt);
                
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(inputFile, true), "UTF-8"));
                String newAccount = usernameInput + "," + passwordInput + "," + saltStr;
                output.newLine();
                output.append(newAccount);
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            loginOrSignup(false);
        }
    }

    private static boolean checkSignupInput(String ary[]){
        String password = ary[1];
        String keyboard = "qwertyuiopasdfghjklzxcvbnm";
        String inverseKeyboard = "mnbvcxzlkjhgfdsapoiuytrewq";
        Pattern letterPatter = Pattern.compile("[a-z ]", Pattern.CASE_INSENSITIVE);
        Pattern digitPatter = Pattern.compile("[0-9 ]");
        boolean validInputs = true;
        boolean consecutiveInts, inverseConsecutiveInts, consecutiveChars, inverseConsecutiveChars, consecutiveKeyboard, inverseConsecutiveKeyboard;
        char prevChar = password.charAt(0);
        int count = 0;
        int[] intAry = new int[4];
        int[] charAry = new int[4];
        int[] keyboardAry = new int[keyboard.length()];
        int[] inverseKeyboardAry = new int[inverseKeyboard.length()];
        for(int j=0; j < keyboard.length(); j++){
            keyboardAry[j] = keyboard.charAt(j);
            inverseKeyboardAry[j] = inverseKeyboard.charAt(j);
        }

        try{
            Scanner in = new Scanner(new File("commonPasswords.txt"));
            while(in.hasNextLine()){
                String s = in.nextLine();
                if(password.equals(s)){
                    validInputs = false;
                    System.out.println("This password is too common");
                }
            }
            in.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        if(password.length() < 8){
            validInputs = false;
            System.out.println("Password must be at least 8 characters long");
        }
        if(password.contains(",")){
            validInputs = false;
            System.out.println("Password must not contain commas");
        }
        if(!letterPatter.matcher(password).find()){
            validInputs = false;
            System.out.println("Password must contain a letter");
        }
        if(!digitPatter.matcher(password).find()){
            validInputs = false;
            System.out.println("Password must contain a digit");
        }
        // check for consectutive digets, letters, and keyboard strokes
        for(int i = 0; i < password.length(); i++){
            if(i<password.length()-3){
                if(Character.isDigit(password.charAt(i)) && 
                Character.isDigit(password.charAt(i+1)) &&
                Character.isDigit(password.charAt(i+2)) && 
                Character.isDigit(password.charAt(i+3)) ){
                    intAry[0] = password.charAt(i);
                    intAry[1] = password.charAt(i+1);
                    intAry[2] = password.charAt(i+2);
                    intAry[3] = password.charAt(i+3);
                    consecutiveInts = true;
                    inverseConsecutiveInts = true;
                    for(int j = 0; j < 3; j++){
                        if(intAry[j]+1 != intAry[j+1]){
                            consecutiveInts = false;
                        }
                    }
                    for(int j = 0; j < 3; j++){
                        if(intAry[j]-1 != intAry[j+1]){
                            inverseConsecutiveInts = false;
                        }
                    }
                    if(consecutiveInts || inverseConsecutiveInts){
                        validInputs = false;
                        System.out.println("Password must not contain consecutive digits");
                    }
                } else {
                    intAry = new int[4];
                }
            }
            if(i<password.length()-3){
                if(Character.isLetter(password.charAt(i)) &&
                Character.isLetter(password.charAt(i+1)) &&
                Character.isLetter(password.charAt(i+2)) &&
                Character.isLetter(password.charAt(i+3)) ){
                    charAry[0] = password.charAt(i);
                    charAry[1] = password.charAt(i+1);
                    charAry[2] = password.charAt(i+2);
                    charAry[3] = password.charAt(i+3);
                    consecutiveChars = true;
                    inverseConsecutiveChars = true;
                    consecutiveKeyboard = true;
                    inverseConsecutiveKeyboard = true;
                    for(int j = 0; j < 3; j++){
                        if(charAry[j]+1 != charAry[j+1]){
                            consecutiveChars = false;
                        }
                    }
                    for(int j = 0; j < 3; j++){
                        if(charAry[j]-1 != charAry[j+1]){
                            inverseConsecutiveChars = false;
                        }
                    }
                    if(consecutiveChars || inverseConsecutiveChars){
                        validInputs = false;
                        System.out.println("Password must not contain consecutive letters");
                    }

                    for(int j = 0; j < keyboardAry.length; j++){
                        for(int x = 0; x < 3; x++){
                            if(keyboardAry[j] == charAry[x]){
                                if(j != keyboardAry.length-1){
                                    if(keyboardAry[j+1] != charAry[x+1]) consecutiveKeyboard = false;
                                } else {
                                    if(consecutiveKeyboard) consecutiveKeyboard = false;
                                }
                            }
                        }
                    }
                    for(int j = 0; j < inverseKeyboardAry.length-1; j++){
                        for(int x = 0; x < 3; x++){
                            if(inverseKeyboardAry[j] == charAry[x]){
                                if(j != inverseKeyboardAry.length-1){
                                    if(inverseKeyboardAry[j+1] != charAry[x+1]) inverseConsecutiveKeyboard = false;
                                } else {
                                    if(inverseConsecutiveKeyboard) inverseConsecutiveKeyboard = false;
                                }
                            }
                        }
                    }
                    if(consecutiveKeyboard || inverseConsecutiveKeyboard){
                        validInputs = false;
                        System.out.println("Password must not contain consecutive keyboard inputs");
                    }
                    
                } else {
                    charAry = new int[4];
                }
            }
            if(i>0){
                char currentChar = password.charAt(i);
                if(prevChar == currentChar){
                    count++;
                    if(count > 2){
                        validInputs = false;
                        System.out.println("Password must not contain 4 repeated characters");
                    }
                }else{
                    count = 0;
                    prevChar = currentChar;
                }
            }
        }

        return validInputs;
    }
}