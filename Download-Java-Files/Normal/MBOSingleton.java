package application.MBO;

import application.CTC.CTCSingleton;
import application.TrackController.TrackControllerSingleton;
import application.TrackModel.TrackLine;
import application.TrackModel.TrackModelSingleton;
import application.TrainController.TrainControllerSingleton;
import application.TrainModel.TrainModelSingleton;

public class MBOSingleton implements MBOInterface {

	// Singleton Functions (NO TOUCHY!!)
	private static MBOSingleton instance = null;
	private double latitude = 0;
	private double longitude = 0;

	private MBOSingleton() {
	}

	public static MBOSingleton getInstance() {
		if (instance == null) {
			instance = new MBOSingleton();
		}

		return instance;
	}

	// =====================================

	// NOTE: Put your data objects here
	private int count = 0;

	// NOTE: Put some functions here
	public void increment() {
		count++;
	}

	public int getCount() {
		return count;
	}
	// hi
	// NOTE: Singleton Connections (Put changes reads, gets, sets that you want to
	// occur here)
	public int getLocation(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
		return 0;
	}
	
	public double getLatitude()
	{
		return latitude;
	}
	
	public double getLongitude()
	{
		return longitude;
	}
	// WARNING: This Only changes the singleton, not your UI. UI updates occur in
	// your UI controller
	public void update() {
		// Example: get the count from a singleton and replace yours with the largest
		//TrainControllerSingleton trnCtrlSin = TrainControllerSingleton.getInstance();
		TrainModelSingleton trainModelSingleton = TrainModelSingleton.getInstance();
		
	}

	@Override
	public void importLine(TrackLine trackLine) {
		// TODO Auto-generated method stub
		
	}

}
