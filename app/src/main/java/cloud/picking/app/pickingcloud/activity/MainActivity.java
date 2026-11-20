package cloud.picking.app.pickingcloud.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.SharedPref;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private Context c;
    private NavigationView navigationView;
    private SharedPref sharedPref;
    private TextView versionView;
    private TextView welcomeView;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.c = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        this.sharedPref = new SharedPref(this.c);
        JSONObject auth = this.sharedPref.getAuth();
        JSONObject license = this.sharedPref.getLicense();

        this.navigationView = (NavigationView) findViewById(R.id.nav_view);
        this.navigationView.setNavigationItemSelectedListener(this);

        if (auth != null) {
            this.navigationView.getMenu().findItem(R.id.nav_ubicazione)
                    .setVisible("1".equals(auth.optString("app_ubicazioni", "0"))
                            || "true".equalsIgnoreCase(auth.optString("app_ubicazioni", "false")));
            this.navigationView.getMenu().findItem(R.id.nav_oridini_coda)
                    .setVisible("1".equals(auth.optString("app_ordini", "0"))
                            || "true".equalsIgnoreCase(auth.optString("app_ordini", "false")));
            this.navigationView.getMenu().findItem(R.id.nav_liste)
                    .setVisible("1".equals(auth.optString("app_liste", "0"))
                            || "true".equalsIgnoreCase(auth.optString("app_liste", "false")));
        }

        View headerView = this.navigationView.getHeaderView(0);
        try {
            this.welcomeView = (TextView) headerView.findViewById(R.id.welcome_view);
            if (auth != null) {
                this.welcomeView.setText(getString(R.string.welcome_label, auth.getString("ui")));
            }
            this.versionView = (TextView) headerView.findViewById(R.id.version_view);
            this.versionView.setText("Versione App 1.7");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!this.sharedPref.isFirstSyncPerformed()) {
            new AlertDialog.Builder(this.c)
                    .setTitle(R.string.sincronizzazione)
                    .setMessage(Html.fromHtml(getString(R.string.messaggio_prima_sincronizzazione)))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.c.startActivity(new Intent(MainActivity.this.c, SyncActivity.class));
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_exit) {
            new AlertDialog.Builder(this.c)
                    .setTitle(getString(R.string.conferma))
                    .setMessage(getString(R.string.messaggio_conferma_uscita))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.sharedPref.logout();
                            MainActivity.this.startActivity(new Intent(MainActivity.this.c, LoginActivity.class));
                            MainActivity.this.finish();
                        }
                    })
                    .show();
        } else if (id == R.id.nav_sync) {
            startActivity(new Intent(this.c, SyncActivity.class));
        } else if (id == R.id.nav_ubicazione) {
            startActivity(new Intent(this.c, UbicazioneActivity.class));
        } else if (id == R.id.nav_oridini_coda) {
            Intent i = new Intent(this.c, OrdiniActivity.class);
            i.putExtra(OrdiniActivity.STATO_KEY, OrdiniActivity.STATO_CODA);
            startActivity(i);
        } else if (id == R.id.nav_prodotti) {
            startActivity(new Intent(this.c, ProdottiActivity.class));
        } else if (id == R.id.nav_liste) {
            startActivity(new Intent(this.c, ListeActivity.class));
        } else if (id == R.id.nav_impostazioni) {
            startActivity(new Intent(this.c, ImpostazioniActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
