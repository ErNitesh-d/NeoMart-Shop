package com.ecom;

import java.util.Scanner;

public class Test {

	public static void main(String[] args) {

		Scanner sc = new Scanner(System.in);
		int cases = sc.nextInt();
		int[] pin = new int[cases];
		for (int i = 0; i < cases; i++) {
			pin[i] = sc.nextInt();
		}

		for (int i = 0; i < cases; i++) {
			int sumOfDigits = String.valueOf(pin[i]).chars().map(Character::getNumericValue).sum();
			if (sumOfDigits % 4 == 0) {
				System.out.println("OPEN");
			} else {
				System.out.println("LOCKED");
			}
		}

		sc.close();
	}

}
