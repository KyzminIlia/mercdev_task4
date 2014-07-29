import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PhotoActivity extends FragmentActivity {
	PhotoFragment photoFragment = new PhotoFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		photoFragment = (PhotoFragment) getSupportFragmentManager()
				.findFragmentByTag(PhotoFragment.FRAGMENT_TAG);
		if (photoFragment == null) {
			photoFragment = new PhotoFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, photoFragment,
							PhotoFragment.FRAGMENT_TAG).commit();
		}
	}
}
