#!bin/bash
#make sure https://github.com/martijnbastiaan/sprockell is installed alongside in ./
mkdir bin
ghc ptest.hs -isprockell/src -o bin/ptest
