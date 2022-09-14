function UtilsModule() {

    let obj = {};

    /**
     * 工具类
     * @constructor
     */
    obj.Utils = function () {
    }

    /**
     * 获取请求参数
     * @returns {null|Map<any, any>}
     */
    obj.Utils.getRequestParams = function () {
        let url = location.href;
        // console.log('url', url);
        let index = url.indexOf("?");
        if (index <= 0) {
            return new Map();
        }

        let map = new Map();
        let params = url.substring(index + 1, url.length).split('&');
        for (let i = 0, length = params.length; i < length; i++) {
            let nv = params[i].split('=');
            let name = nv[0].trim();
            let value = nv.length == 2 ? nv[1].trim() : null;
            map.set(name, value);
        }
        return map;
    }

    /**
     * 存储器
     * @constructor
     */
    obj.Utils.Storage = function () {
    }

    obj.Utils.Storage.set = function (name, value) {
        obj.Utils.Storage._storageStrategy.set(name, value);
    }

    obj.Utils.Storage.get = function (name) {
        return obj.Utils.Storage._storageStrategy.get(name);
    }

    // session Storage
    // sessionStorage和localStorage的区别是：
    // 1、localStorage没有过期时间
    // 2、sessionStorage针对一个session进行数据存储，生命周期与session相同，当用户关闭浏览器后，数据将被删除。
    obj.Utils.Storage._sessionStorage = function () {
    }

    obj.Utils.Storage._sessionStorage.set = function (name, value) {
        sessionStorage.setItem(name, value)
    }

    obj.Utils.Storage._sessionStorage.get = function (name) {
        return sessionStorage.getItem(name);
    }

    // cookie Storage
    obj.Utils.Storage._cookieStorage = function () {
    }

    obj.Utils.Storage._cookieStorage.set = function (name, value) {
        document.cookie = name + "=" + value;
    }

    obj.Utils.Storage._cookieStorage.get = function (name) {
        let cookie = document.cookie;
        let array = cookie.split(';');
        for (let i = 0, length = array.length; i < length; i++) {
            let kv = array[i].split('=');
            if (name === kv[0].trim()) {
                return kv.length >= 2 ? kv[1].trim() : null;
            }
        }
        return null;
    }

    // 设置存储策略
    obj.Utils.Storage._storageStrategy = obj.Utils.Storage._sessionStorage;

    /**
     * Http
     * @constructor
     */
    obj.Utils.Http = function () {
    }

    /**
     * ajax
     * @param url
     * @param data
     * @param type post,delete,put,get
     * @param success
     * @param error
     * @private
     */
    obj.Utils.Http._ajax = function (url, data, type, success, error) {
        $.ajax({
            url: url,
            data: data,
            type: type,
            dataType: "json",
            async: false,
            headers: { 'Content-Type': 'application/json;charset=utf-8' }, //接口json格式
            success: function (resp) {
                // console.log(resp);
                // if (resp.statusCode !== 200) {
                //     if (error) {
                //         error(resp);
                //     } else {
                //         alert(resp.message);
                //     }
                //     return;
                // }
                if (success) {
                    success(resp);
                }
            },
            error: function (resp) {
                if (error) {
                    error(resp);
                    return;
                }
                alert(resp.message);
            }
        });
    }

    /**
     * POST
     * @param url
     * @param data
     * @param success
     * @param error
     */
    obj.Utils.Http.post = function (url, data, success, error) {
        obj.Utils.Http._ajax(url, (typeof data == 'string') && data.constructor == String ? data : JSON.stringify(data), 'post', success, error);
    }

    /**
     * DELETE
     * @param url
     * @param data
     * @param success
     * @param error
     */
    obj.Utils.Http.delete = function (url, data, success, error) {
        obj.Utils.Http._ajax(url, JSON.stringify(data), 'delete', success, error);
    }

    /**
     * PUT
     * @param url
     * @param data
     * @param success
     * @param error
     */
    obj.Utils.Http.put = function (url, data, success, error) {
        obj.Utils.Http._ajax(url, JSON.stringify(data), 'put', success, error);
    }

    /**
     * GET
     * @param url
     * @param data
     * @param success
     * @param error
     */
    obj.Utils.Http.get = function (url, data, success, error) {
        obj.Utils.Http._ajax(url, data, 'get', success, error);
    }

    obj.Utils.Date = function () {
    }

    obj.Utils.Date.expand = function () {
        // 对Date的扩展，将Date转化为指定格式的String
        // 月(M)、日(d)、小时(H)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
        // 年(y)可以用 1-4 个占位符，毫秒(S)只能用1个占位符(是 1-3 位的数字)
        window.Date.prototype.format = function (fmt) {
            let o = {
                "M+": this.getMonth() + 1, // 月份，//获取当前月份(0-11,0代表1月)
                "d+": this.getDate(), // 日，获取当前日(1-31)
                "H+": this.getHours(), // 小时， 获取当前小时数(0-23)
                "m+": this.getMinutes(), // 分，获取当前分钟数(0-59)
                "s+": this.getSeconds(), // 秒，获取当前秒数(0-59)
                "q+": Math.floor((this.getMonth() + 3) / 3), // 季度
                "S": this.getMilliseconds() //毫秒，获取当前毫秒数(0-999)
            };

            if (!(fmt)) {
                fmt = "yyyy-MM-dd HH:mm:ss.S";
            }
            if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
            for (let k in o) {
                if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
            }
            return fmt;
        }

    }

    return obj;
}
