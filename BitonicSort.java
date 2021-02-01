/**
 * Abhay Vivek Kulkarni
 * ak6277
 * CSCI 654
 * Project 2
 * Bitonic Sort
 *
 * Based on MPJ-Express(Java Wrapper for MPI)
 * Version: mpj-v0.44
 * "http://mpj-express.org/download.php"
 *
 */

import mpi.MPI;

import java.util.*;

public class BitonicSort {
    static int MASTER = 0;    // "Main/Master" processor
    static int[] array;
    static int process_rank;
    static int SAMPLING_SIZE = 0;    //Print these many values to check if random array is sorted
    static int SEED = 1000;         // Change this if you want different set of random numbers
    static int BOUND = 1000;        // Upper bound for random numbers

    public static void main(String[] args) {
        //***************************************************************************************
        long start_timer_all = System.nanoTime();
        int sizeOfInputGiven = Integer.parseInt(args[0]);
        for (String s : args) {
            if (s.startsWith("--size")) {
                sizeOfInputGiven = Integer.parseInt(s.replace("--size-", ""));
            }
            if (s.startsWith("--print")) {
                SAMPLING_SIZE = Integer.parseInt(s.replace("--print-", ""));
            }
        }

        boolean result = sizeOfInputGiven > 0 && ((sizeOfInputGiven & (sizeOfInputGiven - 1)) == 0);
        if (!result) {
            System.err.println("The size NEEDS to be a power of 2");
            System.exit(1);
        }
        boolean isPrint = SAMPLING_SIZE > 0;

        int i, j;
        Random rand = new Random();
        //***************************************************************************************
        MPI.Init(args);
        int num_processes = MPI.COMM_WORLD.Size();
        process_rank = MPI.COMM_WORLD.Rank();
        //***************************************************************************************
        // Each processor gets (INPUT_SIZE/P)
        int array_size = sizeOfInputGiven / num_processes;
        array = new int[array_size];
        //***************************************************************************************
        // Each processor has received their individual arrays
        rand.setSeed(SEED);
        for (i = 0; i < array_size; i++) {
            array[i] = rand.nextInt(BOUND);
        }
        MPI.COMM_WORLD.Barrier();
        //***************************************************************************************
        // Number of Stages
        int stages = (int) (Math.log(num_processes) / Math.log(2));
        if (process_rank == MASTER) {
            System.out.println("Number of Processes spawned: \t" + num_processes);
        }

        // Sequential Sort
        // Arrays.sort(array);
        //***************************************************************************************
        // Bitonic Sort
        // Comparisons withing every stage from RAW -> Bitonic -> SORTED
        //
        for (i = 0; i < stages; i++) {
            for (j = i; j >= 0; j--) {
                if (((process_rank >> (i + 1)) % 2 == 0 && (process_rank >> j) % 2 == 0) ||
                        ((process_rank >> (i + 1)) % 2 != 0 && (process_rank >> j) % 2 != 0)) {
                    minCompare(j);
                } else {
                    maxCompare(j);
                }
            }
        }
        //***************************************************************************************
        // Blocks until all processes have finished sorting
        MPI.COMM_WORLD.Barrier();
        if (isPrint) {
            if (process_rank == MASTER) {
                System.out.println("Displaying sorted array of <SAMPLING_SIZE> = " + SAMPLING_SIZE);
                // Print Sorting Results
                System.out.println();
                for (i = 0; i < array_size; i++) {
                    if ((i % (array_size / SAMPLING_SIZE)) == 0) {
                        System.out.println(array[i]);
                    }
                }
            }
        }
        // Done
        MPI.Finalize();
        if(process_rank == 0)
            System.out.println("Everything took: " + (System.nanoTime() - start_timer_all) / 1e9 + " (sec) by--> " + process_rank);
        //***************************************************************************************
        // Sequential
        /*
        long seqTime = System.nanoTime();
        int[] sequentialArray = new int[sizeOfInputGiven];
        for (i = 0; i < array_size; i++) {
            sequentialArray[i] = rand.nextInt(BOUND);
        }
        Arrays.sort(sequentialArray);
        System.out.println("Sequential took: "+(System.nanoTime() - seqTime)/1e9+" s");
        */
        //***************************************************************************************

    }

    // Min Comparator
    static void minCompare(int j) {
        int i;
        int[] min = new int[1];
        int send_counter = 0;
        int[] buffer_send = new int[array.length + 1];
        //***************************************************************************************
        MPI.COMM_WORLD.Send(array, 0, 1, MPI.INT, process_rank ^ (1 << j), 0);
        int[] buffer_receive = new int[array.length + 1];
        MPI.COMM_WORLD.Recv(min, 0, 1, MPI.INT, process_rank ^ (1 << j), 0);
        //***************************************************************************************
        for (i = 0; i < array.length; i++) {
            if (array[i] > min[0]) {
                buffer_send[send_counter + 1] = array[i];
                send_counter++;
            } else {
                break;
            }
        }
        buffer_send[0] = send_counter;
        //***************************************************************************************
        MPI.COMM_WORLD.Send(buffer_send, 0, send_counter, MPI.INT, (process_rank ^ (1 << j)), 0);
        MPI.COMM_WORLD.Recv(buffer_receive, 0, array.length, MPI.INT, (process_rank ^ (1 << j)), 0);
        //***************************************************************************************
        for (i = 1; i < buffer_receive[0] + 1; i++) {
            if (array[array.length - 1] < buffer_receive[i]) {
                array[array.length - 1] = buffer_receive[i];
            } else {
                break;
            }
        }
        //***************************************************************************************
        // Optimization for the processor subarray
        // Decreases the communication overhead by minimizing swaps
        // Based on: "https://www.cs.cmu.edu/~scandal/nesl/alg-sequence.html#bitonicsort"
        Arrays.sort(array);
    }

    // Max Comparator
    static void maxCompare(int j) {
        int i;
        int[] max = new int[1];
        int recv_counter;
        int[] buffer_recieve = new int[array.length + 1];
        //***************************************************************************************
        MPI.COMM_WORLD.Recv(max, 0, 1, MPI.INT, (process_rank ^ (1 << j)), 0);
        int send_counter = 0;
        int[] buffer_send = new int[array.length + 1];
        MPI.COMM_WORLD.Send(array, 0, 1, MPI.INT, (process_rank ^ (1 << j)), 0);
        //***************************************************************************************
        for (i = 0; i < array.length; i++) {
            if (array[i] < max[0]) {
                buffer_send[send_counter + 1] = array[i];
                send_counter++;
            } else {
                break;
            }
        }
        //***************************************************************************************
        MPI.COMM_WORLD.Recv(buffer_recieve, 0, array.length, MPI.INT, (process_rank ^ (1 << j)), 0);
        recv_counter = buffer_recieve[0];
        buffer_send[0] = send_counter;
        MPI.COMM_WORLD.Send(buffer_send, 0, send_counter, MPI.INT, (process_rank ^ (1 << j)), 0);
        //***************************************************************************************
        for (i = 1; i < recv_counter + 1; i++) {
            if (buffer_recieve[i] > array[0]) {
                array[0] = buffer_recieve[i];
            } else {
                break;
            }
        }
        //***************************************************************************************
        // Optimization for the processor subarray
        // Decreases the communication overhead by minimizing swaps
        // Based on: "https://www.cs.cmu.edu/~scandal/nesl/alg-sequence.html#bitonicsort"
        Arrays.sort(array);
        //***************************************************************************************
    }


}