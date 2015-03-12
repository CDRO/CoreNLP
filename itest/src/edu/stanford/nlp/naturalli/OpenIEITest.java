package edu.stanford.nlp.naturalli;

import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Test the natural logic OpenIE extractor at {@link edu.stanford.nlp.naturalli.OpenIE}.
 *
 * @author Gabor Angeli
 */
public class OpenIEITest {
  protected static StanfordCoreNLP pipeline = new StanfordCoreNLP(new Properties(){{
    setProperty("annotators", "tokenize,ssplit,pos,lemma,depparse,natlog,openie");
    setProperty("ssplit.isOneSentence", "true");
    setProperty("tokenize.class", "PTBTokenizer");
    setProperty("tokenize.language", "en");
    setProperty("enforceRequirements", "true");
  }});

  public CoreMap annotate(String text) {
    Annotation ann = new Annotation(text);
    pipeline.annotate(ann);
    return ann.get(CoreAnnotations.SentencesAnnotation.class).get(0);
  }

  public void assertExtracted(String expected, String text) {
    boolean found = false;
    Collection<RelationTriple> extractions = annotate(text).get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
    for (RelationTriple extraction : extractions) {
      if (extraction.toString().equals("1.0\t" + expected)) {
        found = true;
      }
    }
    assertTrue("The extraction '" + expected + "' was not found in '" + text + "'", found);
  }

  public void assertExtracted(Set<String> expected, String text) {
    Collection<RelationTriple> extractions = annotate(text).get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);
    Set<String> guess = extractions.stream().filter(x -> x.confidence > 0.1).map(RelationTriple::toString).collect(Collectors.toSet());
    assertEquals(StringUtils.join(expected.stream().sorted(), "\n").toLowerCase(), StringUtils.join(guess.stream().map( x -> x.substring(x.indexOf("\t") + 1) ).sorted(), "\n").toLowerCase());
  }

  public void assertEntailed(String expected, String text) {
    boolean found = false;
    Collection<SentenceFragment> extractions = annotate(text).get(NaturalLogicAnnotations.EntailedSentencesAnnotation.class);
    for (SentenceFragment extraction : extractions) {
      if (extraction.toString().equals(expected)) {
        found = true;
      }
    }
    assertTrue("The sentence '" + expected + "' was not entailed from '" + text + "'", found);
  }


  @Test
  public void testAnnotatorRuns() {
    annotate("all cats have tails");
  }

  @Test
  public void testBasicEntailments() {
    assertEntailed("some cats have tails", "some blue cats have tails");
    assertEntailed("blue cats have tails", "some blue cats have tails");
    assertEntailed("cats have tails",      "some blue cats have tails");
  }

  @Test
  public void testBasicExtractions() {
    assertExtracted("cats\thave\ttails", "some cats have tails");
  }

  @Test
  public void testExtractionsObamaWikiOne() {
    assertExtracted(new HashSet<String>() {{
      add("Barack Hussein Obama II\tis 44th and current President of\tUnited States");
      add("Barack Hussein Obama II\tis 44th President of\tUnited States");
      add("Barack Hussein Obama II\tis current President of\tUnited States");
      add("Barack Hussein Obama II\tis President of\tUnited States");
      add("Barack Hussein Obama II\tis\tPresident");
      add("Barack Hussein Obama II\tis\tcurrent President");
      add("Barack Hussein Obama II\tis\t44th President");
    }}, "Barack Hussein Obama II is the 44th and current President of the United States, and the first African American to hold the office.");
  }

  @Test
  public void testExtractionsObamaWikiTwo() {
    assertExtracted(new HashSet<String>() {{
      add("Obama\tis graduate of\tColumbia University");
      add("Obama\tis graduate of\tHarvard Law School");
      add("Obama\tborn in\tHonolulu Hawaii");
      add("he\tserved as\tpresident of Harvard Law Review");
      add("he\tserved as\tpresident");
      add("Obama\tis\tgraduate");
    }}, "Born in Honolulu, Hawaii, Obama is a graduate of Columbia University and Harvard Law School, where he served as president of the Harvard Law Review.");
  }

  @Test
  public void testExtractionsObamaWikiThree() {
    assertExtracted(new HashSet<String>() {{
      add("He\twas\tcommunity organizer in Chicago");
      add("He\twas\tcommunity organizer");
      add("He\tearning\tlaw degree");
    }}, "He was a community organizer in Chicago before earning his law degree.");
  }

  @Test
  public void testExtractionsObamaWikiFour() {
    assertExtracted(new HashSet<String>() {{
      add("He\tworked as\tcivil rights attorney");
      add("He\tworked as\trights attorney");
      add("He\ttaught\tconstitutional law");
      add("He\ttaught\tlaw");
      add("He\ttaught at\tUniversity of Chicago Law School");
      add("He\ttaught at\tUniversity of Chicago Law School from 1992");
      add("He\ttaught at\tUniversity");
      add("He\ttaught to\t2004");  // shouldn't be here, but sometimes appears?
    }}, "He worked as a civil rights attorney and taught constitutional law at the University of Chicago Law School from 1992 to 2004.");
  }

  @Test
  public void testExtractionsObamaWikiFive() {
    assertExtracted(new HashSet<String>() {{
      add("He\tserved\tthree terms");
      add("He\trepresenting\t13th District in Illinois Senate");
      add("He\trepresenting\t13th District");
      add("He\trepresenting\tDistrict in Illinois Senate");
      add("He\trepresenting\tDistrict");
      add("He\trunning unsuccessfully for\tUnited States House of Representatives in 2000");
      add("He\trunning unsuccessfully for\tUnited States House of Representatives");
      add("He\trunning unsuccessfully for\tUnited States House");
      add("He\trunning for\tUnited States House of Representatives in 2000");
      add("He\trunning for\tUnited States House of Representatives");
      add("He\trunning for\tUnited States House");
    }}, "He served three terms representing the 13th District in the Illinois Senate from 1997 to 2004, running unsuccessfully for the United States House of Representatives in 2000.");
  }

  @Test
  public void testExtractionsObamaWikiSix() {
    assertExtracted(new HashSet<String>() {{
      add("He\tdefeated\tRepublican nominee John McCain");
      add("He\tdefeated\tnominee John McCain");
      add("He\twas inaugurated as\tpresident on January 20 2009");
      add("He\twas inaugurated as\tpresident");
    }}, "He then defeated Republican nominee John McCain in the general election, and was inaugurated as president on January 20, 2009.");
  }
}
