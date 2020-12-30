package com.akapps.oasisutilities.recyclerviews;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.akapps.oasisutilities.activities.ReportInfo;
import com.akapps.oasisutilities.classes.Helper;
import com.akapps.oasisutilities.classes.Report;
import com.akapps.oasisutilities.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;

public class Report_Recyclerview extends RecyclerView.Adapter<Report_Recyclerview .MyViewHolder>{

    private ArrayList<Report> allReports; // report data
    private Context context;              // activity info
    private FirebaseFirestore db;         // database

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView report_Day;
        private TextView report_Month;
        private TextView report_Year;
        private TextView report_Status;
        private LinearLayout project_Status_Indicator;
        private View view;

        public MyViewHolder(View v) {
            super(v);
            report_Month = v.findViewById(R.id.report_Month);
            report_Year = v.findViewById(R.id.report_Year);
            report_Day = v.findViewById(R.id.report_Day);
            report_Status = v.findViewById(R.id.report_Status);
            project_Status_Indicator = v.findViewById(R.id.report_Status_Indicator);
            view = v;
        }
    }

    public Report_Recyclerview (ArrayList<Report> reports, FirebaseFirestore db) {
        this.allReports = reports;
        this.db = db;
    }

    @Override
    public Report_Recyclerview .MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(parent.getContext()).inflate(R.layout.report_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        context = parent.getContext();
        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        // retrieves the current project data
        final Report currentReport = allReports.get(position);
        String date = currentReport.getMonth() + "_" + currentReport.getDay() + "_" + currentReport.getYear();
        boolean isCustomReport = currentReport.isCustomReport();

        // populates TextViews with report info
        holder.report_Year.setText(String.valueOf(currentReport.getYear()));
        holder.report_Day.setText(String.valueOf(currentReport.getDay()));
        holder.report_Month .setText(String.valueOf(currentReport.getMonth()));

        // if the report is a custom report (report that is made of reports of a specified
        // date range), then the recyclerview changes the TextViews for each respective report
        if(currentReport.isCustomReport()) {
            // custom report is a specific year report (ex: 2020)
            if (currentReport.getMonth() == 0) {
                holder.report_Month.setText("All Reports for: ");
                holder.report_Day.setVisibility(View.GONE);
            }
            // custom report is a specific range of dates
            else if (currentReport.getMonth() > 12) {
                holder.report_Day.setText("to");
                holder.report_Month.setText(currentReport.getStartDate());
                holder.report_Year.setText(currentReport.getEndDate());
                date = currentReport.getStartDate().replace("-", "") +
                        "_0_" + currentReport.getEndDate().replace("-", "");
            }
            // custom report is a specific month and year report (ex: 11-2020)
            else if(currentReport.getDay()==0)
                holder.report_Day.setVisibility(View.GONE);
        }

        // if project is completed, then the text and ImageView is set to green to indicate so
        if(currentReport.isCompleted()) {
            holder.report_Status.setText("Completed");
            holder.report_Status.setTextColor(context.getColor(R.color.green));
            holder.project_Status_Indicator.setBackgroundColor(context.getColor(R.color.green));
        }

        String finalDate1 = date;
        // on clicking report, it opens ReportInfo activity (where user can edit a report)
        holder.view.setOnClickListener(view -> {
            context = view.getContext();
            Intent open_Project = new Intent(context, ReportInfo.class);
            open_Project.putExtra("currentReport", finalDate1);
            open_Project.putExtra("customReport", isCustomReport);
            open_Project.putExtra("currentStore", currentReport.getStore());
            context.startActivity(open_Project);
        });

        String finalDate = date;
        // on long click, user can delete report
        holder.view.setOnLongClickListener(v -> {
            new MaterialDialog.Builder(context)
                        .title(finalDate.replace("_", "-"))
                        .titleColor(context.getColor(R.color.orange_red))
                        .contentGravity(GravityEnum.CENTER)
                        .content("Delete Report?")
                        .contentColor((context.getColor(R.color.colorPrimaryDark)))
                        .backgroundColor(context.getResources().getColor(R.color.black))
                        .positiveText("CLOSE")
                        .canceledOnTouchOutside(false)
                        .autoDismiss(false)
                        .onPositive((dialog, which) -> dialog.dismiss())
                        .onNeutral((dialog, which) -> {
                            if(isCustomReport)
                                db.collection(context.getString(R.string.custom_reports_keyword) + "_" + currentReport.getStore()).document(finalDate).delete();
                            else
                                db.collection(context.getString(R.string.reports_keyword) + "_" + currentReport.getStore()).document(finalDate).delete();
                            notifyDataSetChanged();
                            Helper.showUserMessage(holder.view, "Deleted " + finalDate.replace("_", "-"), Snackbar.LENGTH_LONG, false);
                            dialog.dismiss();
                        })
                        .neutralText("DELETE")
                        .positiveColor(context.getColor(R.color.gray))
                        .neutralColor(context.getColor(R.color.red))
                        .show();

            return false;
        });
    }

    @Override
    public int getItemCount() {
        return allReports.size();
    }

}