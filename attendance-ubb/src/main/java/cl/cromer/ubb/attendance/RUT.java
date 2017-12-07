package cl.cromer.ubb.attendance;

import android.text.TextUtils;

import java.util.Locale;

/**
 * This class will handle anything related to RUT/RUN processing. Most of the methods in this
 * class require a clean RUT to work correctly. So run the cleanRut method before passing it to
 * other methods.
 *
 * RUT corresponds to a company.
 * RUN corresponds to a person.
 * @author Chris Cromer
 * Copyright 2013 - 2016
 */
final public class RUT {
	/**
	 * Returned if the number provided is a RUN.
	 */
	public static final int IS_RUN = 1;
	/**
	 * Returned if the number provided is a RUT.
	 */
	public static final int IS_RUT = 2;

	/**
	 * This method will erase all the periods, dashes, and any other junk in the RUT/RUN,
	 * and will return a cleaned-up string.
	 * @param rut The RUT/RUN that you wish to have cleaned.
	 * @return String The cleaned-up RUT/RUN.
	 */
	public static String cleanRut(String rut) {

		// Change the K to uppercase
		rut = rut.toUpperCase(Locale.getDefault());

		// Remove everything but the letter k and numbers
		rut = rut.replaceAll("\\W|[a-j]|[l-z]", "");
		rut = rut.trim();

		return rut;
	}

	/**
	 * This method will add the periods and dashes to a RUT/RUN, and will return a String with,
	 * the newly formatted RUT/RUN.
	 * @param rut The RUT/RUN that you want to dirty-up.
	 * @return String The dirty RUT/RUN.
	 */
	public static String dirtyRut(String rut) {
		String newRut = "";
		if (rut != null) {
			int length = rut.length() - 1;

			int j = 1;
			for (int i = length; i >= 0; i--) {
				if (i == length) {
					newRut = "-" + rut.charAt(i);
				}
				else {
					if (j == 4) {
						newRut = "." + newRut;
						j = 1;
					}
					j++;
					newRut = rut.charAt(i) + newRut;
				}
			}
		}

		return newRut;
	}

	/**
	 * This method will tell you if the string supplied is a RUT or a RUN.
	 * @param rut The RUT/RUN that you want to see what type it is.
	 * @return int This will return IS_RUN(1) or IS_RUT(2)
	 */
	public static int rutType(String rut) {
		// Let's convert the rut into an integer and cut off the identifier.
		int rutInt = Integer.valueOf(rut.substring(0, rut.length() - 1));
		if (rutInt < 100000000 && rutInt > 50000000) {
			// Business
			return IS_RUT;
		}
		else {
			// Person
			return IS_RUN;
		}
	}

	/**
	 * This method will return the verifier from a given RUT/RUN string.
	 * @param rut The RUT/RUN that you need the verifier from.
	 * @return String Returns the verifier digit. 1-9 or K.
	 */
	public static String getVerifier(String rut) {
		// Super simple, just get the last character.
		if (rut.length() > 0) {
			return rut.substring(rut.length() - 1, rut.length());
		}
		return null;
	}

	/**
	 * This method will generate the verifier for a RUT/RUN.
	 * @param rut The RUT/RUN that you want the verifier for.
	 * @return String Returns the verifier based on the RUT/RUN. Returns null if the string does not contain only numbers
	 */
	public static String generateVerifier(String rut) {

		 /* 1. Multiplicar cada dígito del RUT se por 2, 3, ..., 7, 2, 3, ... de atrás hacia adelante.
		 * 2. Sumar las multiplicaciones parciales.
		 * 3. Calcular el resto de la división por 11
		 * 4. El Dígito Verificador es 11 menos el resultado anterior. Si es 10, se cambia por 'k'.
		 */
		if (!TextUtils.isDigitsOnly(rut)) {
			// The rut is not numeric, return null
			return null;
		}

		// Initialize some values
		int multiplier = 2;
		int sum = 0;
		int remainder;
		int division;
		int rutLength = rut.length();

		// Steps 1 and 2
		for (int i = rutLength - 1; i >= 0; i--) {
			sum = sum + (Integer.valueOf(rut.substring(i, i + 1)) * multiplier);
			multiplier++;
			if (multiplier == 8) {
				multiplier = 2;
			}
		}

		// Step 3
		division = sum / 11;
		division = division * 11;
		remainder = sum - division;

		// Step 4
		if (remainder != 0) {
			remainder = 11 - remainder;
		}

		// Let's return their verifier
		if (remainder == 10) {
			// Their verifier is 10 so let's return K.
			return "K";
		}
		else {
			return String.valueOf(remainder);
		}
	}

	/***
	 * Checks if a given RUT/RUN is valid by checking the verifier, but does not check
	 * the length of the RUT/RUN
	 * @param rut The RUT/RUN that you want to check the validity.
	 * @return boolean Returns true if it's valid or false if it isn't.
	 */
	public static boolean isValidRut(String rut) {
		// By default run an strict check
		return isValidRut(rut, true);
	}

	/***
	 * Checks if a given RUT/RUN is valid by checking the verifier.
	 * @param rut The RUT/RUN that you want to check the validity.
	 * @param strictCheck If true it will verify the length of the RUT/RUN.
	 * @return boolean Returns true if it's valid or false if it isn't.
	 */
	public static boolean isValidRut(String rut, boolean strictCheck) {
		String passedVerifier = getVerifier(rut);
		if (rut.length() > 1 && passedVerifier != null) {
			// Cut off the verifier
			String newRut = rut.substring(0, rut.length() - 1);
			if (passedVerifier.equals(generateVerifier(newRut))) {
				if (strictCheck) {
					if (rut.length() == 8 || rut.length() == 9) {
						return true;
					}
				}
				else {
					return true;
				}
			}
		}
		return false;
	}
}