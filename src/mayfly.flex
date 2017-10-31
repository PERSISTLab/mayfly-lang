package edu.clemson.mayfly.parser;
/**
 * Lexer generator for Mayfly
 * 
 * @author Josiah Hester <josiah@northwestern.edu>
 */


/* --------------------------Usercode Section------------------------ */

import java_cup.runtime.*;
      
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class MayflyLexer
%public

/*
  The current line number can be accessed with the variable yyline
  and the current column number with the variable yycolumn.
*/
%line
%column
    
/* 
   Will switch to a CUP compatibility mode to interface with a CUP
   generated parser.
*/
%cup
   
/*
  Declarations
   
  Code between %{ and %}, both of which must be at the beginning of a
  line, will be copied letter to letter into the lexer class source.
  Here you declare member variables and functions that are used inside
  scanner actions.  
*/
%{   
    /* To create a new java_cup.runtime.Symbol with information about
       the current token, the token will have no value in this
       case. */
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }
    
    /* Also creates a new java_cup.runtime.Symbol with information
       about the current token, but this object has a value. */
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}
   

/*
  Macro Declarations
  
  These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/
   
/* A line terminator is a \r (carriage return), \n (line feed), or
   \r\n. */
LineTerminator = \r|\n|\r\n
   
/* White space is a line terminator, space, tab, or line feed. */
WhiteSpace     = {LineTerminator} | [ \t\f]
		
/* comments */
InputCharacter = [^\r\n]

Comment = {TraditionalComment} | {EndOfLineComment} | {DocumentationComment}

TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
// Comment can be the last line of the file, without line terminator.
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?
DocumentationComment = "/**" {CommentContent} "*"+ "/"
CommentContent       = ( [^*] | \*+ [^/*] )*
   
%%
/* ------------------------Lexical Rules Section---------------------- */
   
/*
   This section contains regular expressions and actions, i.e. Java
   code, that will be executed when the scanner matches the associated
   regular expression. */
   
   /* YYINITIAL is the state at which the lexer begins scanning.  So
   these regular expressions will only be matched if the scanner is in
   the start state YYINITIAL. */
   
<YYINITIAL> {
   
    /* Return the token SEMI declared in the class sym that was found. */
    ";"                { return symbol(sym.SEMI); }
   
    /* Print the token found that was declared in the class sym and then
       return it. */
    "{:"                        { return symbol(sym.LCURLY_COLON); }
    ":}"                        { return symbol(sym.RCURLY_COLON); }
    "{"                         { return symbol(sym.LCURLY); }
    "}"                         { return symbol(sym.RCURLY); }
    "["                         { return symbol(sym.LBRACE); }
    "]"                         { return symbol(sym.RBRACE); }        
    "("                         { return symbol(sym.LPAREN); }
    ")"                         { return symbol(sym.RPAREN); }
    ","                         { return symbol(sym.COMMA); }
    "=>"                        { return symbol(sym.ARROW); }
    "->"                        { return symbol(sym.PIPE); }
    [0-9]+                      { return symbol(sym.NUMBER, new Integer(yytext())); }
    [0-9]+["hr"|"min"|"ms"|"s"] { return symbol(sym.TIME, new String(yytext())); }
    [a-zA-Z_*][a-zA-Z0-9_*]*    { return symbol(sym.IDENTIFIER, new String(yytext()));}
    
    /* comments */
    {Comment}                   { /* ignore */ }
    
    /* Don't do anything if whitespace is found */
    {WhiteSpace}                { /* just skip what was found, do nothing */ }   
}


/* No token was found for the input so through an error.  Print out an
   Illegal character message with the illegal character that was found. */
[^]                    { throw new Error("Illegal character <"+yytext()+">"); }
