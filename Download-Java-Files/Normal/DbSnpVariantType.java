package org.broadinstitute.hellbender.utils.variant;/** * Enum to hold the possible types of dbSnps.  Note that these correspsond to the names used * in the dbSnp database with the exception of indel (which is in-del in dbSnp). */public enum DbSnpVariantType {    SNP, insertion, deletion}