package com.tb.rita.delivery;

import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tb.rita.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import domain.Alias;
import domain.Command;
import domain.custom.MyDialogBox;
import domain.models.CommandDescriptionViewModel;

/**
 * Created by thales on 04/11/17.
 */

public class CommandDescriptionActivity extends AppCompatActivity {


    // Views
    private ListView alias_list;
    private int id_cmd;
    private CommandDescriptionViewModel cmdDescrModel;
    private BluetoothDevice connectedDevice;
    private BluetoothSocket bluetoothSocket;
    private boolean connected = false;
    private UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final String CMD_ID = "ID OF THE SELECTED COMMAND";
    public static final String ALIAS_ID = "ID OF THE SELECTED ALIAS";
    private final int REQUEST_BLUETOOTH_ON = 1;
    private final int REQUEST_BLUETOOTH_CONNECTION = 2;

    private boolean bluetoothOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_descr_screen);
        // Initialize class properties
        Intent intent = getIntent();
        id_cmd = intent.getIntExtra(CMD_ID, -1);
        alias_list = findViewById(R.id.descr_alias_list);

        cmdDescrModel = ViewModelProviders.of(this).get(CommandDescriptionViewModel.class);
        cmdDescrModel.createDb();
        cmdDescrModel.init(id_cmd);
        subscribeData();

        ImageButton btnRun = findViewById(R.id.descr_run);
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
    }

    private void subscribeData() {
        cmdDescrModel.command.observe(this, new Observer<Command>() {
            @Override
            public void onChanged(@Nullable Command command) {
                populateCmdName(command);
            }
        });

        cmdDescrModel.aliases.observe(this, new Observer<List<Alias>>() {
            @Override
            public void onChanged(@Nullable List<Alias> aliases) {
                populateAliasList(aliases);
            }
        });
    }
    // ========================= Transition Functions
    public void onNewAliasButtonPressed(View view) {
        Intent toNewAlias = new Intent(this, NewAliasActivity.class);
        toNewAlias.putExtra(CMD_ID, id_cmd);
        startActivity(toNewAlias);
    }

    public void OnBackButtonPressed(View view) {
        Intent toCmdList = new Intent(this, CommandsListActivity.class);
        startActivity(toCmdList);
    }

    public void onAliasPressed(int alias_id, View view) {
        Intent intent = new Intent(this, NewAliasActivity.class);
        intent.putExtra(CMD_ID, id_cmd);
        intent.putExtra(NewAliasActivity.IS_EDIT, true);
        intent.putExtra(ALIAS_ID, alias_id);
        startActivity(intent);
    }

    // ========================= Dataview Functions
    private void populateAliasList(List<Alias> aliases) {
        if(aliases == null)
            aliases = new ArrayList<>();
        final ArrayAdapter<Alias> adapter = new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, aliases);
        alias_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onAliasPressed(adapter.getItem(i).getId_alias(), view);
            }
        });
        alias_list.setAdapter(adapter);
    }

    private void populateCmdName(Command cmd) {
        if(cmd == null)
            cmd = new Command("", 1);
        TextView cmdNameView = findViewById(R.id.descr_cmd_name);
        cmdNameView.setText(cmd.getName());
    }

    private void showDialog() {
        String cmdName = cmdDescrModel.command.getValue().getName();
        MyDialogBox diagBox = new MyDialogBox();
        diagBox.setDialogMessage(getString(R.string.run_cmd_popup) + " " + cmdName);
        diagBox.setBuilder(new AlertDialog.Builder(this));
        diagBox.getBuilder().setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Run cmd
                checkBluetooth();

            }
        });
        diagBox.getBuilder().setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Back to activity screen
            }
        });
        diagBox.show(this.getFragmentManager(), "test");
    }

    private boolean checkBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothOn = bluetoothAdapter.isEnabled();
        if(bluetoothAdapter == null) {
            Toast.makeText(this, "Sem adaptador bluetooth no dispositivo",
                    Toast.LENGTH_LONG).show();
            return false;
        } else if(!bluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, REQUEST_BLUETOOTH_ON);
        } else if(bluetoothAdapter.isEnabled()) {
            if(connected) {
                try {
                    bluetoothSocket.close();
                    connected = false;
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Erro ao fechar conexão", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Intent conectaDispositvo = new Intent(this, ListBluetoothDevices.class);
                startActivityForResult(conectaDispositvo, REQUEST_BLUETOOTH_CONNECTION);
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_BLUETOOTH_ON: {
                if(resultCode == RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooh ativado com sucesso!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Não foi possível ativar o bluetooth", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_BLUETOOTH_CONNECTION: {
                if(resultCode == RESULT_OK) {
                    String mac = data.getStringExtra(ListBluetoothDevices.MAC_ADRESS);
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(mac);

                    try {
                        bluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(myUUID);
                        bluetoothSocket.connect();
                        connected = true;
                        Toast.makeText(getApplicationContext(), "Conexão realizada!", Toast.LENGTH_LONG).show();
                    } catch (IOException e)  {
                        connected = false;
                        Toast.makeText(getApplicationContext(), "Falha ao conectar", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    connected = false;
                    Toast.makeText(getApplicationContext(), "Falha ao obter MAC", Toast.LENGTH_LONG).show();
                }
                break;
            }
            default: {
                bluetoothOn = false;
                connected = false;
                Toast.makeText(this, "ERROR", Toast.LENGTH_LONG);
            }
        }
    }
}
