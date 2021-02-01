# parallel_bitonic_sort
Performs bitonic sort parallely

# Bitonic Sort

## Code parameters:

-np P            


## VM parameter for assigning P processors 

--size-X 

## Program parameter:

X is a number that's a power of 2 (1024,2048,...) 

It represents the size of input data (<= 268,435,456)

If X is not a power of 2; The code will do a System.exit(1) and print a error message.

## (OPTIONAL)

--print-N 

N is the number of rows the code will print for verifying the correctness of the algorithm.
(10,15,...) for easy readability; can go upto size of array.

If not given the code only prints the time taken to execute.



The code was used to test the data on:

1024,2048,... 268M

using 

1,2,4,8 processors


< Report + Diagrams + Plots + Results >are all present in the report.pdf file
