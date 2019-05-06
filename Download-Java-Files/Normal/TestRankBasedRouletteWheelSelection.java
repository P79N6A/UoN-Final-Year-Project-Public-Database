package selection;

import org.junit.Test;
import base.Population;
import base.Tour;
import selection.RankBasedRouletteWheelSelection;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertTrue;

public class TestRankBasedRouletteWheelSelection {

    @Test
    public void doSelection(){

        Population population = new Population();
        ArrayList<Tour> tours = new ArrayList<>();
        Tour child01 = new Tour();
        Tour child02 = new Tour();
        tours.add(child01);
        tours.add(child02);
        population.setTourList(tours);

        RankBasedRouletteWheelSelection selection = new RankBasedRouletteWheelSelection();


        tours =  selection.doSelection(population);
        assertTrue( selection.doSelection(population) != null);

        LinkedHashSet hashSet = new LinkedHashSet();

        hashSet.addAll(tours);

        assertTrue(hashSet.size()==tours.size());



    }
}