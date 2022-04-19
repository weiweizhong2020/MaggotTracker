#!/bin/bash
java -classpath compiled:lib/ij.jar:lib/jmf.jar:lib/commons-io-2.4.jar:lib/commons-math-2.0.jar org.wormloco.mag.MagRecognizer $1 $2

