package com.apps.jlee.carcare.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.apps.jlee.carcare.Objects.Gas;
import com.apps.jlee.carcare.R;
import com.apps.jlee.carcare.Data.SQLiteDatabaseHandler;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GraphFragment extends Fragment
{
    private SQLiteDatabaseHandler db;
    private List<Entry> entries;
    private List<Object> list;
    private LineChart chart;
    private String[] values;
    private ProgressBar oilPB, brakesPB, wheelsPB, batteryPB, timingBeltPB;
    private String dbDateFormat = "yyyy-MM-dd HH:mm:ss";
    private TextView t, oilProgress, brakesProgress, wheelsProgress, batteryProgress, timeingBeltProgress;
    private double previous_oil_total = 0, previous_brakes_total = 0, previous_wheels_total = 0, previous_battery_total = 0, previous_timingbelt_total = 0;

    public GraphFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        db = SQLiteDatabaseHandler.getInstance(getContext());
        entries = new ArrayList<Entry>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        chart = v.findViewById(R.id.chart);
        oilPB = v.findViewById(R.id.oilPB);
        oilProgress = v.findViewById(R.id.oilprogress);
        brakesPB = v.findViewById(R.id.brakesPB);
        brakesProgress = v.findViewById(R.id.brakesprogress);
        wheelsPB = v.findViewById(R.id.wheelsPB);
        wheelsProgress = v.findViewById(R.id.wheelsprogress);
        batteryPB = v.findViewById(R.id.batteryPB);
        batteryProgress = v.findViewById(R.id.batteryprogress);
        timingBeltPB = v.findViewById(R.id.timingBeltPB);
        timeingBeltProgress = v.findViewById(R.id.timingbeltprogress);
        t = v.findViewById(R.id.title);

        oilPB.setMax(3000);
        brakesPB.setMax(50000);
        wheelsPB.setMax(15000);
        batteryPB.setMax(30000);
        timingBeltPB.setMax(100000);

        //new AsyncDBTask(db).execute();
        list = db.getAllEntries();
        if(list.size() != 0)
        {
            loadGraphData(1);
            updateProgressBar();
            loadProgressBar();
        }

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        // Inflate the menu; this adds items to the action bar.
        inflater.inflate(R.menu.actionbar_home_fragment, menu);
    }

    //Called when an item inside action bar is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        entries.clear();
        chart.clear();

        switch(item.getItemId())
        {
            case R.id.GraphByCost: t.setText("Cost Graph");loadGraphData(1);break;
            case R.id.GraphByGallons: t.setText("Gallons Graph");loadGraphData(2);break;
            case R.id.GraphByMiles: t.setText("Miles Graph");loadGraphData(3);break;
            case R.id.GraphByMPG: t.setText("MPG Graph");loadGraphData(4);break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadGraphData(int graphType)
    {
        SimpleDateFormat format = new SimpleDateFormat(dbDateFormat);
        values = new String[list.size()];

        switch (graphType)
        {
            case 1:
                for (int i = 0; i < list.size(); i++)
                {
                    Date date = new Date(((Gas)(list.get(i))).getDateRefilled());

                    entries.add(new Entry(i,(float)((Gas)(list.get(i))).getCost()));
                    values[i] = (String) DateFormat.format("M",date) + "/" + (String) DateFormat.format("dd",date);
                }
                break;

            case 2:
                for (int i = 0; i < list.size(); i++)
                {
                    Date date = new Date(((Gas)(list.get(i))).getDateRefilled());

                    entries.add(new Entry(i, (float) ((Gas) (list.get(i))).getAmount()));
                    values[i] = (String) DateFormat.format("M", date) + "/" + (String) DateFormat.format("dd", date);
                }
                break;
            case 3:
                for (int i = 0; i < list.size(); i++)
                {
                    Date date = new Date(((Gas)(list.get(i))).getDateRefilled());

                    entries.add(new Entry(i, (float) ((Gas) (list.get(i))).getMiles()));
                    values[i] = (String) DateFormat.format("M", date) + "/" + (String) DateFormat.format("dd", date);
                }
                break;
            case 4:
                for (int i = 0; i < list.size(); i++)
                {
                    Date date = new Date(((Gas)(list.get(i))).getDateRefilled());

                    entries.add(new Entry(i, (float) ((Gas) (list.get(i))).getMiles() / (float) ((Gas) (list.get(i))).getAmount()));
                    values[i] = (String) DateFormat.format("M", date) + "/" + (String) DateFormat.format("dd", date);
                }
                break;
        }
        drawGraph();
    }

    public void drawGraph()
    {
        LineDataSet dataSet = new LineDataSet(entries, "Label");
        dataSet.setLineWidth(3);
        dataSet.setDrawValues(false);
        dataSet.setColor(Color.rgb(0,255,47));
        dataSet.setCircleHoleColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setCircleHoleRadius(5f);
        dataSet.setCircleRadius(5f);

        LineData lineData = new LineData(dataSet);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(15f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter()
        {
            // "value" represents the position of the label on the axis (x or y) which is why we need to store the values into a string array.
            public String getFormattedValue(float value, AxisBase axis)
            {
                if (value >= 0)
                {
                    if (values.length > (int) value)//This prevents IndexOutofArray Exception
                        return values[(int)value];
                    else
                        return "";
                }
                else
                    return "";
            }
        });

        xAxis.setLabelRotationAngle(45);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextSize(15f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawZeroLine(true);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);

        /*Similar to setting margins around chart*/
        chart.setExtraOffsets(0, 0, 0, 10);

        chart.setData(lineData);
        chart.invalidate();

        chart.setVisibleXRangeMaximum(8);/*Set Maximum x values being displayed*/
        chart.moveViewToX(list.size());
    }

    public void updateProgressBar()
    {
        SharedPreferences sharedpreferences = getContext().getSharedPreferences("Replacement Values", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();

        /*Create checkpoint if it doesn't exist*/
        if(sharedpreferences.getLong("oil_checkpoint",0) == 0)
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

        if (previous_oil_total < 3000)
            editor.putFloat("oil", (float)previous_oil_total);
        else
        {
            editor.putFloat("oil", 3000);
            previous_oil_total = 3000;
            //scheduleNotification("Oil Replacement", "You have driven more than 3000 miles and your oil needs to be replaced. Have you replaced your oil?",1);
        }

        if (previous_brakes_total < 50000)
            editor.putFloat("brakes", (float)previous_brakes_total);
        else
        {
            editor.putFloat("brakes", 50000);
            previous_brakes_total = 50000;
            //scheduleNotification("Brake Replacement", "You have driven more than 50000 miles and your brakes needs to be replaced. Have you replaced your brakes?",2);
        }

        if (previous_wheels_total < 15000)
            editor.putFloat("wheels", (float)previous_wheels_total);
        else
        {
            editor.putFloat("wheels", 15000);
            previous_wheels_total = 15000;
            //scheduleNotification("Tire Replacement", "You have driven more than 15000 miles and your tires needs to be replaced. Have you replaced your tires?",3);
        }

        if (previous_battery_total < 30000)
            editor.putFloat("battery", (float)previous_battery_total);
        else
        {
            editor.putFloat("battery", 30000);
            previous_battery_total = 30000;
            //scheduleNotification("Battery Replacement", "You have driven more than 30000 miles and your battery needs to be replaced. Have you replaced your battery?",4);
        }

        if (previous_timingbelt_total < 100000)
            editor.putFloat("timingbelt", (float)previous_timingbelt_total);
        else
        {
            editor.putFloat("timingbelt", 100000);
            previous_timingbelt_total = 100000;
            //scheduleNotification("Timing Belt Replacement", "You have driven more than 100000 miles and your timing belt needs to be replaced. Have you replaced your timing belt?",5);
        }

        editor.apply();
    }

    public void loadProgressBar()
    {
        oilPB.post(new Runnable()
        {
            @Override
            public void run()
            {
                int oil = (int)previous_oil_total;
                oilPB.setProgress(oil);
                oilProgress.setText(oil+"/3000");
            }
        });

        brakesPB.post(new Runnable()
        {
            @Override
            public void run()
            {
                int brakes = (int)previous_brakes_total;
                brakesPB.setProgress(brakes);
                brakesProgress.setText(brakes+"/50000");
            }
        });

        wheelsPB.post(new Runnable()
        {
            @Override
            public void run()
            {
                int wheels = (int)previous_wheels_total;
                wheelsPB.setProgress(wheels);
                wheelsProgress.setText(wheels+"/15000");
            }
        });

        batteryPB.post(new Runnable()
        {
            @Override
            public void run()
            {
                int battery = (int)previous_battery_total;
                batteryPB.setProgress(battery);
                batteryProgress.setText(battery+"/30000");
            }
        });

        timingBeltPB.post(new Runnable()
        {
            @Override
            public void run()
            {
                int timingBelt = (int)previous_timingbelt_total;
                timingBeltPB.setProgress(timingBelt);
                timeingBeltProgress.setText(timingBelt+"/100000");
            }
        });
    }

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
            return handler.getAllEntries();
        }
        @Override
        protected void onPostExecute(List<Object> DBlist)
        {
            super.onPostExecute(DBlist);

            if(DBlist.size() != 0)
            {
                list = DBlist;
                loadGraphData(1);
                loadProgressBar();
            }
        }
    }
}

