package org.kymjs.blog.ui.fragment;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.kymjs.blog.R;
import org.kymjs.blog.adapter.TweetAdapter;
import org.kymjs.blog.domain.SimpleBackPage;
import org.kymjs.blog.domain.Tweet;
import org.kymjs.blog.domain.TweetsList;
import org.kymjs.blog.ui.SimpleBackActivity;
import org.kymjs.blog.ui.widget.listview.PullToRefreshBase;
import org.kymjs.blog.ui.widget.listview.PullToRefreshBase.OnRefreshListener;
import org.kymjs.blog.ui.widget.listview.PullToRefreshList;
import org.kymjs.blog.utils.Parser;
import org.kymjs.blog.utils.UIHelper;
import org.kymjs.kjframe.KJHttp;
import org.kymjs.kjframe.http.HttpCallBack;
import org.kymjs.kjframe.http.HttpConfig;
import org.kymjs.kjframe.http.HttpParams;
import org.kymjs.kjframe.ui.BindView;
import org.kymjs.kjframe.utils.KJLoger;
import org.kymjs.kjframe.utils.StringUtils;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * [吐槽]界面
 * 
 * @author kymjs
 * 
 */
public class TweetFragment extends TitleBarFragment {

    public static final String TAG = TweetFragment.class.getSimpleName();

    @BindView(id = R.id.listview)
    private PullToRefreshList mRefreshLayout;
    private ListView mListView;

    private TweetAdapter adapter;

    private KJHttp kjh;
    private final Set<Tweet> tweets = new TreeSet<Tweet>();
    private final String OSCTWEET_HOST = "http://www.oschina.net/action/api/tweet_list?pageSize=20&pageIndex=";

    public static final int REQUEST_CODE_RECORD = 1;
    public static final int REQUEST_CODE_IMAGE = 2;
    public static final String CONTENT_KEY = "Tweet_content_key";
    public static final String AUDIOPATH_KEY = "Tweet_audiopath_key";
    public static final String IMAGEPATH_KEY = "Tweet_imagepath_key";

    @Override
    protected View inflaterView(LayoutInflater inflater, ViewGroup container,
            Bundle bundle) {
        View root = View.inflate(outsideAty,
                R.layout.frag_pull_refresh_listview, null);
        return root;
    }

    @Override
    protected void setActionBarRes(ActionBarRes actionBarRes) {
        super.setActionBarRes(actionBarRes);
        actionBarRes.title = getString(R.string.str_tweet_title);
        actionBarRes.backImageId = R.drawable.titlebar_back;
        actionBarRes.menuImageId = R.drawable.titlebar_add;
    }

    @Override
    protected void initData() {
        super.initData();
        HttpConfig config = new HttpConfig();
        config.cacheTime = 0;
        config.useDelayCache = false;
        kjh = new KJHttp(config);
    }

    @Override
    protected void initWidget(View parentView) {
        super.initWidget(parentView);
        mListView = mRefreshLayout.getRefreshView();
        mListView.setDivider(new ColorDrawable(android.R.color.transparent));
        mListView.setSelector(new ColorDrawable(android.R.color.transparent));
        mRefreshLayout.setPullLoadEnabled(true);
        mRefreshLayout.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onPullDownToRefresh(
                    PullToRefreshBase<ListView> refreshView) {
                refresh(0);
            }

            @Override
            public void onPullUpToRefresh(
                    PullToRefreshBase<ListView> refreshView) {
                refresh();
            }
        });

        fillUI();
    }

    /**
     * 首次进入时填充数据
     */
    private void fillUI() {
        refresh(0);
    }

    /**
     * 刷新
     * 
     * @param page
     *            请求第几页数据
     */
    private void refresh() {
        double page = tweets.size() / 20;
        page += 1.9; // 因为服务器返回的可能会少于20条，所以采用小数进一法加载下一页
        refresh((int) page);
    }

    private void refresh(int page) {
        kjh.get(OSCTWEET_HOST + page, new HttpCallBack() {
            @Override
            public void onSuccess(String t) {
                super.onSuccess(t);
                KJLoger.debug(TAG + "网络请求" + t);
                List<Tweet> datas = Parser.xmlToBean(TweetsList.class, t)
                        .getList();
                tweets.addAll(datas);
                if (adapter == null) {
                    adapter = new TweetAdapter(mListView, tweets,
                            R.layout.item_list_tweet);
                    mListView.setAdapter(adapter);
                } else {
                    adapter.refresh(tweets);
                }
                mRefreshLayout.onPullDownRefreshComplete();
                mRefreshLayout.onPullUpRefreshComplete();
            }
        });
    }

    @Override
    public void onBackClick() {
        super.onBackClick();
        outsideAty.finish();
    }

    @Override
    public void onMenuClick() {
        super.onMenuClick();
        SimpleBackActivity.postShowForResult(this, 1,
                SimpleBackPage.OSC_TWEET_SEND);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            return;
        }
        switch (resultCode) {
        case REQUEST_CODE_RECORD:
            handleSubmit(data.getStringExtra(CONTENT_KEY), null,
                    data.getStringExtra(AUDIOPATH_KEY));
            break;
        case REQUEST_CODE_IMAGE:
            handleSubmit(data.getStringExtra(CONTENT_KEY),
                    new File(data.getStringExtra(IMAGEPATH_KEY)), null);
            break;
        default:
            break;
        }
    }

    /**
     * 发布动弹
     */
    private void handleSubmit(String strSpeech, File imageFile, String audioPath) {
        HttpConfig config = new HttpConfig();
        config.cacheTime = 0;
        KJHttp kjh = new KJHttp(config);
        HttpParams params = new HttpParams();
        params.putHeaders("cookie", UIHelper.getUser(outsideAty).getCookie());
        params.put("uid", UIHelper.getUser(outsideAty).getUid());
        params.put("msg", strSpeech + "————来自[爱看博客]APP");

        if (imageFile != null && imageFile.exists()) {
            params.put("img", imageFile);
        }
        if (!StringUtils.isEmpty(audioPath)) {
            params.put("amr", new File(audioPath));
        }
        kjh.post("http://www.oschina.net/action/api/tweet_pub", params,
                new HttpCallBack() {
                    @Override
                    public void onPreStar() {
                        super.onPreStar();
                        // 设置上传动画
                    }

                    @Override
                    public void onSuccess(String t) {
                        super.onSuccess(t);
                        KJLoger.debug("发表动弹:" + t);
                        // 隐藏上传动画
                    }

                    @Override
                    public void onFailure(int errorNo, String strMsg) {
                        super.onFailure(errorNo, strMsg);
                        // 设置上传动画失败图标
                    }
                });
    }
}
