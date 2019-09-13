package com.apps.jlee.carcare.Fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.apps.jlee.carcare.Adapters.GasAdapter;
import com.apps.jlee.carcare.Broadcast_Receivers.AlarmReceiver;
import com.apps.jlee.carcare.Dialog_Fragments.FilterDialogFragment;
import com.apps.jlee.carcare.Objects.Gas;
import com.apps.jlee.carcare.Dialog_Fragments.GasDialogFragment;
import com.apps.jlee.carcare.R;
import com.apps.jlee.carcare.Data.SQLiteDatabaseHandler;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class GasFragment extends Fragment
{
    private GasDialogFragment d;
    private FilterDialogFragment f;
    private ArrayList<HashMap<String,String>> arrayList;
    private List<Object> gasList;
    private RecyclerView rv;
    private GasAdapter adapter;
    private SQLiteDatabaseHandler db;
    private boolean updateFlag = false;
    private String DateFormat = "M/dd/yy";
    private int edit_Position = 0;

    public GasFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        arrayList = new ArrayList<>();
        gasList = new LinkedList<>();
        d = new GasDialogFragment();
        f = new FilterDialogFragment();
        db = new SQLiteDatabaseHandler(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
       View view = inflater.inflate(R.layout.fragment_gas2, container, false);
       FloatingActionButton fab = view.findViewById(R.id.fab2);

       rv = view.findViewById(R.id.gas_Entries2);
       rv.setLayoutManager(new LinearLayoutManager(getContext()));
       adapter = new GasAdapter(gasList);
       rv.setAdapter(adapter);
       new AsyncDBTask(db).execute();

       fab.setOnClickListener(new View.OnClickListener()
       {
            public void onClick(View view)
            {
                updateFlag = false;
                Bundle b = new Bundle();
                b.putString("Cost","");
                b.putString("Miles","");
                b.putString("Gallons","");
                b.putString("Date","");
                d.setArguments(b);
                d.show(getFragmentManager(), "fragment_gas");
            }
       });

       d.setListener(new GasDialogFragment.GasInterface()
       {
           @Override
           public void onClick(String milesValue, String gallonsValue, String cost, Date date)
           {
               int dbCount = (int)db.getProfilesCount(new Gas())+1;

               //Insert new Gas Entry
               if(!updateFlag)
               {
                   Gas g = new Gas();
                   g.setID(dbCount);
                   g.setMiles(Double.parseDouble(milesValue));
                   g.setAmount(Double.parseDouble(gallonsValue));
                   g.setCost(Double.parseDouble(cost));
                   g.setDateRefilled(date.getTime());
                   db.addEntry(g);
                   gasList.add(g);
               }
               //Update existing Gas Entry
               else
               {
                    Gas g = (Gas)gasList.get(edit_Position);
                    g.setCost(Double.valueOf(cost));
                    g.setMiles(Double.valueOf(milesValue));
                    g.setAmount(Double.valueOf(gallonsValue));
                    g.setDateRefilled(date.getTime());
                    db.updateEntry(g);
               }
               updateProgressBar();
               adapter.notifyDataSetChanged();
           }
       });

       f.setListener(new FilterDialogFragment.FilterInterface()
       {
           @Override
           public void onClick(Date starting_date, Date ending_date, String sortBy)
           {
               new AsyncDBFilterTask(db,starting_date,ending_date,sortBy).execute();
           }
       });

       return view;
    }

    //Called when an item inside context menu is selected
    public boolean onContextItemSelected(MenuItem item)
    {
        if(item.getItemId()==121)
        {
            int index = item.getGroupId();
            updateFlag = true;
            edit_Position = index;

            Bundle b = new Bundle();
            b.putString("ID",((Gas)gasList.get(index)).getID()+"");
            b.putString("Cost",((Gas)gasList.get(index)).getCost()+"");
            b.putString("Miles",((Gas)gasList.get(index)).getMiles()+"");
            b.putString("Gallons",((Gas)gasList.get(index)).getAmount()+"");
            b.putString("Date",((Gas)gasList.get(index)).getDateRefilled()+"");
            d.setArguments(b);
            d.show(getFragmentManager(), "fragment_gas");
        }
        else if(item.getItemId() == 122)
        {
            int index = item.getGroupId();

            db.deleteEntry(new Gas(((Gas)gasList.get(index)).getID(),0,0,0,0));
            gasList.remove(index);

            updateProgressBar();
            adapter.notifyDataSetChanged();
        }
        else
            return false;

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Inflate the menu; this adds items to the action bar.
        inflater.inflate(R.menu.actionbar_gas_fragment, menu);
    }

    //Called when an item inside action bar is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if(id == R.id.Filter)
        {
            f.show(getFragmentManager(), "fragment_filter");
        }
        else if (id == R.id.Email)
        {
            new AsyncExcel(db).execute();
            //email();

            //For s9, we are saving excel files on the interal storage but this storage has internal and external partitions. Internal partitions are invisible while external are not.

            //Internal Storage:
            // Files saved to the internal storage are private to your application and other applications cannot access them. When the user uninstalls your application,
            // these files are removed/deleted. Your app user also can't access them using file manager; even after enabling "show hidden files" option in file manager. To access files in
            // Internal Storage, you have to root your Android phone.

            //External Storage:
            //This can be a removable storage media (such as an SD card) or an internal (non-removable) storage

            //The following methods give paths to the external paritition of the internal storage
            //Log.v("Dodgers",Environment.getExternalStorageDirectory().getAbsolutePath());
            //Log.v("Dodgers",Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath());

            //Following two will give you the directory paths under the package name: Android/data/com.apps.jlee.carcare
            //Log.v("Dodgers",getContext().getExternalCacheDir().getAbsolutePath());
            //Log.v("Dodgers",getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath());
        }
        else
        {
            generateGasEntries();
        }

        return super.onOptionsItemSelected(item);
    }

    public void generateGasEntries()
    {
        HashMap<String,String> hashMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        Double cost,gallons,miles;
        int id;

        for(int i = 1; i < 14; i++)
        {
            id = (int)db.getProfilesCount(new Gas())+1;
            cal.set(Calendar.MONTH,6); cal.set(Calendar.DAY_OF_MONTH, i*2); cal.set(Calendar.YEAR, 2019);

            cost = Double.parseDouble(String.format("%.2f",(new Random().nextInt(10)+45) + new Random().nextDouble()));
            gallons = Double.parseDouble(String.format("%.2f",(new Random().nextInt(6)+10) + new Random().nextDouble()));
            miles = Double.parseDouble(String.format("%.2f",(new Random().nextInt(115)+400) + new Random().nextDouble()));

            db.addEntry(new Gas(id,cost,gallons,miles,cal.getTime().getTime()));
        }
    }

    public void updateProgressBar()
    {
        int previous_oil_total = 0, previous_brakes_total = 0, previous_wheels_total = 0, previous_battery_total = 0, previous_timingbelt_total = 0;
        List<Object> list = db.getAllEntries(new Gas());
        SharedPreferences sharedpreferences = getContext().getSharedPreferences("Replacement Values", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        if(list.size() == 1)
        {
            editor.putLong("oil_checkpoint",((Gas) (list.get(0))).getDateRefilled());
            editor.putLong("brakes_checkpoint",((Gas) (list.get(0))).getDateRefilled());
            editor.putLong("wheels_checkpoint",((Gas) (list.get(0))).getDateRefilled());
            editor.putLong("battery_checkpoint",((Gas) (list.get(0))).getDateRefilled());
            editor.putLong("timingbelt_checkpoint",((Gas) (list.get(0))).getDateRefilled());
        }

        for (int i = 0; i < list.size(); i++)
        {
            double miles = ((Gas) (list.get(i))).getMiles();

            if(((Gas) (list.get(i))).getDateRefilled() >= sharedpreferences.getLong("oil_checkpoint",0))
                previous_oil_total += miles;
            if(((Gas) (list.get(i))).getDateRefilled() >= sharedpreferences.getLong("brakes_checkpoint",0))
                previous_brakes_total += miles;
            if(((Gas) (list.get(i))).getDateRefilled() >= sharedpreferences.getLong("wheels_checkpoint",0))
                previous_wheels_total += miles;
            if(((Gas) (list.get(i))).getDateRefilled() >= sharedpreferences.getLong("battery_checkpoint",0))
                previous_battery_total += miles;
            if(((Gas) (list.get(i))).getDateRefilled() >= sharedpreferences.getLong("timingbelt_checkpoint",0))
                previous_timingbelt_total += miles;
        }
        if(list.size() > 0)
        {
            if (previous_oil_total < 3000)
                editor.putFloat("oil", previous_oil_total);
            else
            {
                editor.putFloat("oil", 3000);
                scheduleNotification("Oil Replacement", "You have driven more than 3000 miles and your oil needs to be replaced. Have you replaced your oil?",1);
            }

            if (previous_brakes_total < 50000)
                editor.putFloat("brakes", previous_brakes_total);
            else
            {
                editor.putFloat("brakes", 50000);
                scheduleNotification("Brake Replacement", "You have driven more than 50000 miles and your brakes needs to be replaced. Have you replaced your brakes?",2);
            }

            if (previous_wheels_total < 15000)
                editor.putFloat("wheels", previous_wheels_total);
            else
            {
                editor.putFloat("wheels", 15000);
                scheduleNotification("Tire Replacement", "You have driven more than 15000 miles and your tires needs to be replaced. Have you replaced your tires?",3);
            }

            if (previous_battery_total < 30000)
                editor.putFloat("battery", previous_battery_total);
            else
            {
                editor.putFloat("battery", 30000);
                scheduleNotification("Battery Replacement", "You have driven more than 30000 miles and your battery needs to be replaced. Have you replaced your battery?",4);
            }

            if (previous_timingbelt_total < 100000)
                editor.putFloat("timingbelt", previous_timingbelt_total);
            else
            {
                editor.putFloat("timingbelt", 100000);
                scheduleNotification("Timing Belt Replacement", "You have driven more than 100000 miles and your timing belt needs to be replaced. Have you replaced your timing belt?",5);
            }
        }
        else
        {
            editor.putFloat("oil", previous_oil_total);
            editor.putFloat("brakes", previous_brakes_total);
            editor.putFloat("wheels", previous_wheels_total);
            editor.putFloat("battery", previous_battery_total);
            editor.putFloat("timingbelt", previous_timingbelt_total);
        }
        editor.apply();
    }

    public void scheduleNotification(String title, String message, int id)
    {/*
        AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        intent.putExtra("title",title);
        intent.putExtra("message",message);
        intent.putExtra("id",id);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(getContext(), id, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 00);

        //Schedule a repeating alarm that runs every 24 hours
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),1000 * 60 * 60 * 24, alarmIntent);
        // Schedule a repeating alarm that runs every minute
        // alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),1000 * 60 * 1, alarmIntent);
        // Schedule alarm that runs once at the given time
        //alarmMgr.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(), alarmIntent);*/
    }

    /*Load Gas Entries*/
    private class AsyncDBTask extends AsyncTask<Void,Void,List<Object>>
    {
        private SQLiteDatabaseHandler handler;

        public AsyncDBTask(SQLiteDatabaseHandler handler)
        {
            this.handler = handler;
        }
        @Override
        protected List<Object> doInBackground(Void... voids)
        {
            return handler.getAllEntries(new Gas());
        }
        @Override
        protected void onPostExecute(List<Object> list)
        {
            super.onPostExecute(list);

            if(list.size() != 0)
            {
                gasList.addAll(list);
                adapter.notifyDataSetChanged();
            }
        }
    }

    /*Sort Gas Entries*/
    private class AsyncDBFilterTask extends AsyncTask<Void,Void,List<Object>>
    {
        private SQLiteDatabaseHandler handler;
        private Date starting_date, ending_date;
        private String sortBy;

        public AsyncDBFilterTask(SQLiteDatabaseHandler handler, Date starting_date, Date ending_date, String sortBy)
        {
            this.handler = handler;
            this.starting_date = starting_date;
            this.ending_date = ending_date;
            this.sortBy = sortBy;
        }

        @Override
        protected List<Object> doInBackground(Void... voids)
        {
            return handler.getAllEntries(new Gas());
        }

        @Override
        protected void onPostExecute(List<Object> list)
        {
            super.onPostExecute(list);
            Date date = null;

            if(list.size() != 0)
            {
                gasList.clear();
                for (int i = 0; i < list.size(); i++)
                    if (starting_date.getTime() <= ((Gas) (list.get(i))).getDateRefilled() && ending_date.getTime() >= ((Gas) (list.get(i))).getDateRefilled())
                        gasList.add(list.get(i));

                Collections.sort(gasList, new MapComparator(sortBy));
                adapter.notifyDataSetChanged();
            }
        }
    }

    /*Load Gas Entries and Generate Excel File*/
    private class AsyncExcel extends AsyncTask<Void,Void,List<Object>>
    {
        private SQLiteDatabaseHandler handler;

        public AsyncExcel(SQLiteDatabaseHandler handler)
        {
            this.handler = handler;
        }

        @Override
        protected List<Object> doInBackground(Void... voids)
        {
            return handler.getAllEntries(new Gas());
        }

        @Override
        protected void onPostExecute(List<Object> list)
        {
            super.onPostExecute(list);

            if(list.size() != 0)
            {
                long yourmilliseconds = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy_HH_mm_ss");
                Date resultdate = new Date(yourmilliseconds);

                String Fnamexls = "excelSheet"+sdf.format(resultdate)+ ".xls";
                File sdCard = getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                File directory = new File(sdCard.getAbsolutePath() + "/Gas_Entries");
                if (!directory.mkdirs()) ;
                    //Log.v("Dodgers", "Directory not created");

                File file = new File(directory, Fnamexls);

                WorkbookSettings wbSettings = new WorkbookSettings();
                wbSettings.setLocale(new Locale("en", "EN"));

                WritableWorkbook workbook;
                try
                {
                    workbook = Workbook.createWorkbook(file, wbSettings);
                    WritableSheet sheet = workbook.createSheet("First Sheet", 0);

                    Label label = new Label(0, 0, "Date");
                    Label label1 = new Label(1,0,"Cost");
                    Label label2 = new Label(2,0,"Miles");
                    Label label3 = new Label(3,0,"Gallons");
                    Label label4 = new Label(4,0,"MPG");

                    try
                    {
                        sheet.addCell(label);
                        sheet.addCell(label1);
                        sheet.addCell(label2);
                        sheet.addCell(label3);
                        sheet.addCell(label4);
                    }
                    catch (RowsExceededException e) {e.printStackTrace();}
                    catch (WriteException e) {e.printStackTrace();}

                    for (int i = 0; i < list.size(); i++)
                    {
                        try
                        {
                            Date date = new Date(((Gas) (list.get(i))).getDateRefilled());
                            Label l = new Label(0, i + 1, new SimpleDateFormat(DateFormat).format(date));
                            Label l1 = new Label(1, i + 1, String.valueOf(((Gas) (list.get(i))).getCost()));
                            Label l2 = new Label(2, i + 1, String.valueOf(((Gas) (list.get(i))).getMiles()));
                            Label l3 = new Label(3, i + 1, String.valueOf(((Gas) (list.get(i))).getAmount()));
                            Label l4 = new Label(4, i + 1, String.format("%.2f", (((Gas) (list.get(i))).getMiles()) / (((Gas) (list.get(i))).getAmount())));

                            sheet.addCell(l);
                            sheet.addCell(l1);
                            sheet.addCell(l2);
                            sheet.addCell(l3);
                            sheet.addCell(l4);
                        } catch (RowsExceededException e)
                        {
                            e.printStackTrace();
                        } catch (WriteException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    workbook.write();
                    try
                    {
                        workbook.close();
                    } catch (WriteException e) {e.printStackTrace();}

                } catch (IOException e) {e.printStackTrace();}

                //Initate the BroadcastReceiver
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getContext());
                EmailBroadCastReceiver emailBroadCastReceiver = new EmailBroadCastReceiver();
                //Intentfilter specify the kind of intents that the component can receive. In this case, the emailBroadCastReceiver(component) will only receive intents with the
                // action "com.action.email"
                IntentFilter filter = new IntentFilter();
                filter.addAction("com.action.email");
                manager.registerReceiver(emailBroadCastReceiver,filter);

                //Broadcast the intent
                Intent i = new Intent("com.action.email");
                i.putExtra("file_path",directory.toString());
                i.putExtra("file_name",Fnamexls);
                manager.sendBroadcast(i);
            }
        }
    }

    //This broadcastreceiver will be used locally instead of globally across the system and called by the AsyncExcel
    class EmailBroadCastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            //Log.v("Dodgers",intent.getStringExtra("file_path") + "/" + intent.getStringExtra("file_name"));
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("application/vnd.ms-excel");
            //emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"jon@example.com"}); // recipients
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Gas Entries");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Here are all the gas entries recorded");
            emailIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getContext(),"com.mydomain.fileprovider",new File(intent.getStringExtra("file_path"),intent.getStringExtra("file_name"))));
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }

    //Defines the rules for comparisons that is used in Collection.sort method
    class MapComparator implements Comparator<Object>
    {
        private final String key;

        public MapComparator(String key)
        {
            this.key = key;
        }

        @Override
        public int compare(Object first, Object second)
        {
            int v = 0;

            switch (key)
            {
                case "MPG": v = (((Gas)(second)).getMiles()/((Gas)(second)).getAmount()+"").compareTo(((Gas)(first)).getMiles()/((Gas)(first)).getAmount()+""); break;
                case "Cost": v = (((Gas)(second)).getCost()+"").compareTo((((Gas)(first)).getCost()+"")); break;
                case "Miles": v = (((Gas)(second)).getMiles()+"").compareTo((((Gas)(first)).getMiles()+"")); break;
                case "Gallons": v = (((Gas)(second)).getAmount()+"").compareTo((((Gas)(first)).getAmount()+"")); break;
            }
            return v;
        }
    }
}