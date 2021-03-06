package org.broadinstitute.hellbender.tools.spark.pipelines;

import htsjdk.samtools.ValidationStringency;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.testutils.SamAssertionUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class BQSRPipelineSparkIntegrationTest extends CommandLineProgramTest {
    private static final class BQSRTest {
        final String referenceURL;
        final String bam;
        final String knownSites;
        final String args;
        final String outputExtension;
        final String expectedFileName;

        private BQSRTest(String referenceURL, String bam, String knownSites, String outputExtension, String args, String expectedFileName) {
            this.referenceURL = referenceURL;
            this.bam = bam;
            this.knownSites = knownSites;
            this.outputExtension = outputExtension;
            this.args = args;
            this.expectedFileName = expectedFileName;
        }

        public String getCommandLine() {
            return  " -R " + referenceURL +
                    " -I " + bam +
                    " " + args +
                    (knownSites.isEmpty() ? "": " --known-sites " + knownSites) +
                    " -O %s";
        }

        @Override
        public String toString() {
            return String.format("BQSR(bam='%s', args='%s')", bam, args);
        }
    }

    private String getResourceDir(){
        return getTestDataDir() + "/" + "BQSR" + "/";
    }

    @DataProvider(name = "BQSRLocalRefTest")
    public Object[][] createBQSRLocalRefTestData() {
        final String GRCh37Ref_2021 = b37_reference_20_21;
        final String hiSeqBam_chr20 = getResourceDir() + WGS_B37_CH20_1M_1M1K_BAM;
        final String dbSNPb37_20 = getResourceDir() + DBSNP_138_B37_CH20_1M_1M1K_VCF;

        final String hiSeqBam_20_21_100000 = getResourceDir() + "CEUTrio.HiSeq.WGS.b37.NA12878.20.21.10m-10m100.bam";
        final String hiSeqCram_20_21_100000 = getResourceDir() + "CEUTrio.HiSeq.WGS.b37.NA12878.20.21.10m-10m100.cram";
        final String more20Sites = getResourceDir() + "dbsnp_138.b37.20.10m-10m100.vcf"; //for testing 2 input files
        final String more21Sites = getResourceDir() + "dbsnp_138.b37.21.10m-10m100.vcf"; //for testing 2 input files

        return new Object[][]{
                // input local, computation local.
                //Note: these output files were created by running GATK3
                {new BQSRTest(GRCh37Ref_2021, hiSeqBam_chr20, dbSNPb37_20, ".bam", "-indels --enable-baq", getResourceDir() + "expected.CEUTrio.HiSeq.WGS.b37.ch20.1m-1m1k.NA12878.recalibrated.DIQ.bam")},

                //Output generated with GATK4 (resulting BAM has 4 differences with GATK3)
                {new BQSRTest(b37_reference_20_21 , hiSeqBam_20_21_100000, more20Sites, ".bam", "-indels --enable-baq --known-sites " + more21Sites, getResourceDir() + "expected.MultiSite.bqsr.pipeline.bam")},
                {new BQSRTest(b37_reference_20_21 , hiSeqCram_20_21_100000, more20Sites, ".cram", "-indels --enable-baq --known-sites " + more21Sites, getResourceDir() + "expected.MultiSite.bqsr.pipeline.cram")},
       };
    }

    @Test(dataProvider = "BQSRLocalRefTest", groups = "spark")
    public void testBQSRLocalRef(BQSRTest params) throws IOException {
        File outFile = GATKBaseTest.createTempFile("bqsrSparkPipelineTest", params.outputExtension);
        final List<String> args = new ArrayList<>();

        args.add("-I");
        args.add(new File(params.bam).getAbsolutePath());
        args.add("-O");
        args.add(outFile.getAbsolutePath());

        File referenceFile = null;
        if (params.referenceURL != null) {
            referenceFile = new File(params.referenceURL);
            args.add("-R");
            args.add(referenceFile.getAbsolutePath());
        }
        args.add("--known-sites");
        args.add(params.knownSites);
        if (params.args != null) {
            Stream.of(params.args.trim().split(" ")).forEach(args::add);
        }

        runCommandLine(args);

        SamAssertionUtils.assertEqualBamFiles(outFile, new File(params.expectedFileName), referenceFile, true, ValidationStringency.SILENT);
    }
}
