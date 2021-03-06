package com.dumpit.ffff;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class itemBarcode extends AppCompatActivity {
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    FirebaseAuth mAuth;
    FirebaseUser user;

    ImageView itemBarcode;
    TextView itemName;
    TextView itemPrice;
    TextView BuyTime;
    TextView buyAfterPoint;
    TextView buyUser;
    Button go_shopping;

    boolean isBought;
    String[] itemNameArray = {"누가바", "돼지바", "자일리톨", "츄파춥스", "호두마루", "체리마루", "지구젤리", "후라보노"};
    String[] storageArray = {"who", "pig", "xylitol", "chu", "hodu", "cherry", "jelly", "hoo"};
    String folderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_barcode);

        itemBarcode = (ImageView) findViewById(R.id.itemBarcode);
        buyUser = (TextView) findViewById(R.id.barcodeU);

        Intent intent = getIntent();
        String itemN = intent.getStringExtra("name");
        int itemP = intent.getIntExtra("price", 0);
        String buyTime = intent.getStringExtra("buyTime");
        int afterPoint = intent.getIntExtra("afterPoint", 0);
        String imageUri = intent.getStringExtra("imageURI");

        itemName = (TextView) findViewById(R.id.barcodeN);
        itemName.setText(itemN);
        itemPrice = (TextView) findViewById(R.id.barcodeP);
        itemPrice.setText(itemP+"P");
        BuyTime = (TextView) findViewById(R.id.barcodeT);
        BuyTime.setText(buyTime);
        buyAfterPoint = (TextView) findViewById(R.id.barcodeAP);
        buyAfterPoint.setText(afterPoint+"P");

        Glide.with(itemBarcode.this).load(imageUri)
                .error(R.drawable.loading)
                .into(itemBarcode);

        // Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        String email = user.getEmail();
        int index = email.indexOf("@");
        String id = email.substring(0, index);
        String web = email.substring(index+1);
        int webidx = web.indexOf(".");
        String website = web.substring(0, webidx);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String getname = snapshot.child("users").child(id+"_"+website).child("nickname").getValue(String.class);
                buyUser.setText(getname);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        for(int i=0; i<itemNameArray.length; i++) {
            if(itemN.equals(itemNameArray[i]))
                folderName = storageArray[i];
        }
        // 아이템 바코드 추가
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference listRef = storage.getReference().child("itemBarcode/"+folderName);
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            item.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        String uri = task.getResult().toString();
                                        isBought = false;
                                        databaseReference.child("buy").addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for(DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                                    if(uri.equals(dataSnapshot.getValue(String.class))) {
                                                        isBought = true;
                                                        break;
                                                    }
                                                }
                                                if(isBought == false) {
                                                    databaseReference.child("buy").child(buyTime).setValue(uri);
                                                    databaseReference.child("users/"+id+"_"+website+"/marketHistory/"+buyTime+"/imageURI").setValue(uri);
                                                }
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(itemBarcode.this, "진입실패", Toast.LENGTH_SHORT).show();
                                }
                            });
                            String uri = item.getDownloadUrl().toString();


                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                    }
                });



        go_shopping = (Button) findViewById(R.id.go_shopping);
        go_shopping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    /**이미지 (파이어베이스 스토리지에서 가져오기) */
    private void getFireBaseProfileImage() {
        //우선 디렉토리 파일 하나만든다.
        File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/itemBoarcode");
        //이미지를 저장할 수 있는 디렉토리
        // 구분할 수 있게 /toolbar_images폴더에 넣어준다.
        // 이 파일안에 저 디렉토리가 있는지 확인
        if (!file.isDirectory()) { //디렉토리가 없으면,
            file.mkdir(); //디렉토리를 만든다.
        }
        downloadImg(); //이미지 다운로드해서 가져오기 메서드
    }
    /**이미지 다운로드해서 가져오기 메서드 */
    private void downloadImg() {
        FirebaseStorage storage = FirebaseStorage.getInstance(); //스토리지 인스턴스를 만들고,
        // 다운로드는 주소를 넣는다.
        StorageReference storageRef = storage.getReference();//스토리지를 참조한다
        storageRef.child("itemBarcode/tempImage.png").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) { //성공시
                Glide.with(itemBarcode.this).load(uri)
                        .error(R.drawable.loading)
                        .into(itemBarcode);
            } }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //실패시
                Toast.makeText(getApplicationContext(), "진입실패.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
