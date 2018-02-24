package com.davidmalakh.autojavatemplate;

public class TestCase {
	/*-
	* FIELDS:
	* this.a --int
	* this.b --int
	* this.c --int
	* this.l --Lower
	* METHODS:
	* this.doIt(String that) --double
	* METHODS FROM FIELDS:
	* this.l.doThing(int thing1, String otherThing1, Double lastThing1) --String
	-*/
    int a;
    int b;
    int c;
    Lower l;

    public TestCase(int a, int b, int c, Lower l) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.l = l;
    }
    
    public double doIt(String that) {
        return 0.0;
    }
}

abstract class ALower {
	/*-
	* FIELDS:
	* this.y --int
	* METHODS:
	* this.doAbstractThing(int thing2) --int
	-*/
    int y;

    public ALower(int y) {
        this.y = y;
    }
    
    public int doAbstractThing(int thing2) {
        return 0;
    }
}

class Lower extends ALower {
	/*-
	* FIELDS:
	* this.x --int
	* this.b --int
	* METHODS:
	* this.doThing(int thing1, String otherThing1, Double lastThing1) --String
	*
	* PLUS EVERYTHING FROM SUPER CLASS:  ALower
	-*/
    int x;
    int b;

    public Lower(int x, int b, int y) {
        super(y);
        this.x = x;
        this.b = b;
    }
    
    public String doThing(int thing1, String otherThing1, Double lastThing1) {
        return "";
    }
}

class NoMethods {
	/*-
	* FIELDS:
	* this.tada --int
	-*/
    int tada;

    public NoMethods(int tada) {
        this.tada = tada;
    }
}
