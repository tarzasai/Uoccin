package net.ggelardi.uoccin.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class Title {

    final protected Session session;

    protected boolean loaded = false;

    public long timestamp = 0;
    public String name;
    public int year;
    public String poster;
    public String plot;
    public String rated;
    public String actors;
    public List<String> genres = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
    public int rating = 0;
    public boolean watchlist = false;

    public Title(Context context) {
        session = Session.getInstance(context);
    }

    public long age() {
        return System.currentTimeMillis() - timestamp;
    }

    public boolean isNew() {
        return timestamp <= 0;
    }

    abstract public boolean isRecent();

    public boolean isLoaded() {
        return loaded;
    }

    public String name() {
        return TextUtils.isEmpty(name) ? "N/A" : name;
    }

    public String year() {
        return year <= 0 ? "N/A" : Integer.toString(year);
    }

    public String plot() {
        if (TextUtils.isEmpty(plot))
            return "N/A";
        return plot;
    }

    public String rated() {
        return TextUtils.isEmpty(rated) ? "N/A" : rated;
    }

    public String actors() {
        return TextUtils.isEmpty(actors) ? "N/A" : actors.replace(",", ", ");
    }

    public String genres() {
        return genres.isEmpty() ? "N/A" : TextUtils.join(", ", genres);
    }

    public String getTags() {
        return tags.isEmpty() ? "N/D" : TextUtils.join(", ", tags);
    }

    public abstract void setTags(String[] value);

    public abstract String people();

    public String rating() {
        return rating <= 0 ? "N/A" : Integer.toString(rating);
    }

    protected String tag() {
        return this.getClass().getSimpleName();
    }

    public void editTags(Activity activity) {
        final LayoutInflater inflater = activity.getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_tags, null);
        final AppCompatMultiAutoCompleteTextView edt = (AppCompatMultiAutoCompleteTextView) view.getRootView();
        final AlertDialog dlg_tags = new AlertDialog.Builder(activity).setTitle(R.string.tagact_title)
                .setView(view).setCancelable(true).setNegativeButton(R.string.dlg_btn_cancel, null)
                .setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setTags(edt.getText().toString().split(",\\s*"));
                    }
                }).create();
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity.getApplicationContext(),
                android.R.layout.simple_dropdown_item_1line, session.getAllTags());
        edt.setAdapter(adapter);
        edt.setTokenizer(new AppCompatMultiAutoCompleteTextView.CommaTokenizer());
        edt.setThreshold(1);
        edt.setDropDownBackgroundResource(R.color.textColorNormal);
        edt.setText(TextUtils.join(", ", tags));
        edt.setHint(R.string.tagact_hint);
        dlg_tags.show();
    }
}
