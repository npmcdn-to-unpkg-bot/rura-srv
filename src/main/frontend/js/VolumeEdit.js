$(document).on('keyup change', '.updates-input', function () {
    var $formItem = $(this).closest('.form-item');
    var $chapterSelect = $formItem.find('select.updates-input');
    var $dateInput = $formItem.find('input.updates-input');
    setNameLabelText($formItem, $dateInput.val().split(' ')[0] + ': ' + $chapterSelect.children(':selected').text().replace('　', ''));
}).on('keyup change', '.member-input', function () {
    var $formItem = $(this).closest('.form-item');
    var $memberInput = $formItem.find('.tt-input');
    var $activitySelect = $formItem.find('select.member-input');
    setNameLabelText($formItem, $memberInput.typeahead('val') + ' - ' + $activitySelect.children(':selected').text());
}).on('change', '.nested-checkbox', function () {
    var $input = $(this);
    var $formItem = $input.closest('.form-item');
    findNameLabel($formItem).toggleClass('sub-chapter', $input.prop('checked'));
}).on('change', '.published-checkbox', function () {
    var $input = $(this);
    var $formItem = $input.closest('.form-item');
    $formItem.find('.publish-date-group').hide().find('.form-control')
        .data("DateTimePicker").date($input.prop('checked') ? moment.tz(window.serverTimeZone).subtract(1, 'days') : null);
});

function setChapterOrderDictionary() {
    window.chapterOrderDictionary = {'-1': -1};
    $('#chapters').find('.form-item').each(function () {
        chapterOrderDictionary[$(this).find('.chapter-id').val()] = $(this).find('.order-number').val();
    });
}

function sortImagesSelectorItems($listGroup) {
    if (!$listGroup)
        $listGroup = $('#images').find('.list-group.select');
    $listGroup.children().sort(function (a, b) {
        return (chapterOrderDictionary[$(a).data('chapter-id')] - chapterOrderDictionary[$(b).data('chapter-id')])
            + ($(a).data('order-number') - $(b).data('order-number')) / 100000
    }).appendTo($listGroup);
}

function initImagesChapterLabels() {
    window.chapterOrderDictionary = {'-1': -1};
    var $listGroup = $('#images').find('.list-group.select');
    $listGroup.find('.heading').detach();
    var $coversHeading = $('<a class="list-group-item heading" data-chapter-id="-1" data-order-number="-1"><span class="move">Обложки</span></a>');
    $listGroup.prepend($coversHeading);
    $('#chapters').find('.form-item').each(function () {
        chapterOrderDictionary[$(this).find('.chapter-id').val()] = $(this).find('.order-number').val();
        var $chapHeading = $coversHeading.clone();
        $chapHeading.data('chapter-id', $(this).find('.chapter-id').val())
            .find('.move').text($(this).find('.name-input').val())
            .toggleClass('sub-chapter', $(this).find('.nested-checkbox').is(':checked'));
        $listGroup.prepend($chapHeading);
    });
    sortImagesSelectorItems($listGroup);
}

$(document).on('dragover', function (e) {
    if (window.dropZoneTimeout)
        clearTimeout(window.dropZoneTimeout);
    else $('body').addClass('dragover'); //выставляем класс на body который активирует css отрисовку дропзон

    // при затягивании файла в одну из дропзон, она подсвечивается
    $('.image-data-main,.image-data-color,#images .list-group.select').removeClass('hover');
    $(e.target).closest('.image-data-main,.image-data-color,#images .list-group.select').addClass('hover');

    // если в течении 800мс событие не повторяется предпологаем что никто уже ничего не перетягивает и выключаем отрисовку дропзон
    window.dropZoneTimeout = setTimeout(function () {
        window.dropZoneTimeout = null;
        $('body').removeClass('dragover');
        $('.image-data-main,.image-data-color,#images .list-group.select').removeClass('hover');
    }, 800);
}).on('drop dragover dragstart', function (e) {
    e.preventDefault();
}); // выключаем стандaртное поведение браузера на drag'n'drop

var $images = $('#images');
var $imagesSelect = $images.find('.list-group.select');
$imagesSelect.on("sortupdate", function (event, ui) { // для списка изображений задаем обработчик перетягивания
    if (ui != undefined && ui.item.hasClass('heading')) // если мы перетянули главу, мы должны убедится что не нарушили порядок следовани глав
    {
        var start = 0; // jquery-ui sortable не дает информации о том какой у елемента был индекс до перетягивания
        $(this).children().each(function (i, e) { // поэтому мы находим его линейно
            if ($(e).position().top <= ui.originalPosition.top) // сравнивая по y координате каждого елемента в пикселах с координатой изначального положения
                start = i;
            else return false;
        });
        var end = ui.item.index(); // индекс итоговой позиции известен
        if (start < end) // если мы перетянули вниз
            $(this).children().slice(start, end).filter('.heading').insertAfter(ui.item); // то перемещаем все заголовки (если они есть) между индексами и ставим их в том же порядке после тепущего
        else
            $(this).children().slice(end + 1, start + 1).filter('.heading').insertBefore(ui.item); // в противном случае точно так же ставим перед текущим
    }
    var order = 0, chapter_id = 0; // теперь нам нужно для каждого изображения поставить его порядковый номер и id главы к которой оно принадлежит
    $(this).children().each(function () { // вообще делать это линейно для всех елементов при любом изменении не очень хорошо. правельные было бы обновлять только для тех изображений для которых что-то изменилось. но мне было лень
        if ($(this).is('.heading'))
            chapter_id = $(this).data('chapter-id'); // если мы попали на елемент заголовка - запомнили номер текущей главы
        else {
            $(this).data('chapter-id', chapter_id);
            $($(this).attr('href')).find('.chapter-id').val(chapter_id); // вспомнили последний известный айдишник главы
            $(this).data('order-number', ++order);
        }
    });
});

$(initImagesChapterLabels);
$('#chapters').on('addnewitem', function (e, d) {
    $(d.form).find('.publish-date-group').each(initPublishDateGroup);
}).find('.list-group.select').on("sortupdate", function (event, ui) {
    setTimeout(function () {
        setChapterOrderDictionary();
        sortImagesSelectorItems();
    }, 100)
}).data('toggle', 'multiple');
$('.publish-date-group').each(initPublishDateGroup);

function initPublishDateGroup() {
    var $input = $(this).find('.form-control').datetimepicker({
        format: 'DD.MM.YYYY HH:mm',
        stepping: 10,
        locale: 'ru',
        icons: {
            time: 'fa fa-clock-o',
            date: 'fa fa-calendar',
            up: 'fa fa-chevron-up',
            down: 'fa fa-chevron-down',
            previous: 'fa fa-chevron-left',
            next: 'fa fa-chevron-right',
            today: 'fa fa-calendar-check-o',
            clear: 'fa fa-trash-o',
            close: 'fa fa-close'
        }
    });
    $(this).toggle(moment.tz(window.serverTimeZone).isBefore($input.data("DateTimePicker").date()));
}

$(function () {
    $('#datetimepicker-plan').datetimepicker({
        format: 'DD.MM.YYYY HH:mm',
        inline: true,
        sideBySide: true,
        minDate: moment.tz(window.serverTimeZone).subtract(1, 'days'),
        stepping: 10,
        locale: 'ru',
        icons: {
            time: 'fa fa-clock-o',
            date: 'fa fa-calendar',
            up: 'fa fa-chevron-up',
            down: 'fa fa-chevron-down',
            previous: 'fa fa-chevron-left',
            next: 'fa fa-chevron-right',
            today: 'fa fa-calendar-check-o',
            clear: 'fa fa-trash-o',
            close: 'fa fa-close'
        }
    });
});
$('#publish-date-modal').find('.btn-primary').click(function () {
    var createUpdates = $('#create-updates-checkbox').prop('checked');
    window.addChapterUpdatesCount = $('#chapters').find('.list-group-item.active').each(function () {
        var chapterFormItem = $($(this).attr('href'));
        chapterFormItem.find('.published-checkbox').prop('checked', false);
        chapterFormItem.find('.publish-date-group').show()
            .find('.form-control').data("DateTimePicker")
            .date($('#datetimepicker-plan').data("DateTimePicker").date());
        if (createUpdates)
            $('#updates').find('.btn-success').click();
    }).length;
    if (!createUpdates)
        window.addChapterUpdatesCount = 0;
});

$('#updates').on('addnewitem', function (e, d) {
        var $updateFormItem = $(d.form);
        if (window.addChapterUpdatesCount-- > 0) {
            var $chapterFormItem = $($('#chapters').find('.list-group-item.active').eq(window.addChapterUpdatesCount).attr('href'));
            $updateFormItem.find('select.updates-input').val($chapterFormItem.find('.chapter-id').val());
            $updateFormItem.find('input.updates-input').val($chapterFormItem.find('.publish-date-group .form-control').val()).trigger('keyup');
        }
        $updateFormItem.find('.input.updates-input').each(initUpdateDateField);
    })
    .find('.input.updates-input').each(initUpdateDateField);
function initUpdateDateField() {
    var $input = $(this).datetimepicker({
        format: 'DD.MM.YYYY HH:mm',
        stepping: 10,
        locale: 'ru',
        icons: {
            time: 'fa fa-clock-o',
            date: 'fa fa-calendar',
            up: 'fa fa-chevron-up',
            down: 'fa fa-chevron-down',
            previous: 'fa fa-chevron-left',
            next: 'fa fa-chevron-right',
            today: 'fa fa-calendar-check-o',
            clear: 'fa fa-trash-o',
            close: 'fa fa-close'
        }
    });
}

//upload


function initFileData(file) {
    file.index = file.$selectorItem.data('internal-index');
    file.$formItem = $(file.$selectorItem.attr('href')).find('.image-data-' + file.ctype);
    file.$formItem.find('input:eq(3)').val(file.name);
    if (file.ctype == 'main') {
        file.$selectorItem.find('.name-label span').text(file.name);
    }
    else {
        file.$formItem.find('.image-data-color-trigger').show();
    }
    showPreviewIfReady(file);
}

function initFormItemFileUpload(formItem) {

    var $formItem = $(formItem);
    var $fileupload = $formItem.find('.btn-image-replace');
    $fileupload.fileupload({
        url: $('#images').data('upload-url'),
        headers: {'Wicket-Ajax': true, 'Wicket-Ajax-BaseURL': Wicket.Ajax.baseUrl, 'Accept': 'application/json'},
        dataType: 'json',
        acceptFileTypes: /(\.|\/)(jpe?g|png)$/i,
        formData: null,
        previewMaxHeight: 180,
        previewMaxWidth: 260,
        maxNumberOfFiles: 1,
        dropZone: $formItem,
        add: function (e, data) {
            if (data.files.length == 1) {
                var file = data.files[0];
                file.$selectorItem = $images.find('.list-group.select .active');
                file.ctype = $formItem.hasClass('image-data-main') ? 'main' : 'color';
                initFileData(file);
                data.formData = {ctype: file.ctype, index: file.index};
                $images.find('.progress').collapse('show'); // показываем прогресбар
                console.log('Wicket.Ajax.post add-url coh', data);
                data.submit(); // начинаем загрузку
            }
        }
    });
    configFileUpload($fileupload);
}

function showPreviewIfReady(file) {
    if (file.preview && file.$formItem) { // заменяем иконку загрузки на превюшку, как только она становится доступна
        if (file.ctype == 'main') {
            var previewClone = $(file.preview).clone().get(0);
            previewClone.getContext('2d').drawImage(file.preview, 0, 0);
            file.$selectorItem.find('img').after(previewClone).detach();
        }
        file.$formItem.find('img').after($(file.preview).addClass('img-responsive')).detach();
    }
}

function configFileUpload(element) {
    $(element).on('fileuploadsubmit', function (e, data) {
        console.log('fileuploadsubmit', data);
    }).on('fileuploadprocessalways', function (e, data) {
        console.log('fileuploadprocessalways', data);
        var file = data.files[data.index];
        showPreviewIfReady(file);
        if (file.error) // или выводим сообщение об ошибке обработки на клиенте
            addAlert($images, 'ERROR', '<strong>Ошибка!</strong> ' + file.error);
    }).on('fileuploadprogressall', function (e, data) { // обновляем прогресбар
        console.log('fileuploadprogressall', data);
        var progress = parseInt(data.loaded / data.total * 100, 10);
        $images.find('.progress-bar').css('width', progress + '%').attr('aria-valuenow', progress)
            .find('span').text(progress + '% Complete');
    }).on('fileuploaddone', function (e, data) { // при завершении загрузки заменяем превюшку на img тег с адресом уже загруженной ирасты
        $images.find('.progress').collapse('hide');
        console.log('fileuploaddone', data);
        if (data.result.error) // выводим ошибку возвращенную с сервера
            addAlert($images, 'ERROR', '<strong>Ошибка!</strong> ' + data.result.error);
        else if (data.result.index)
            Wicket.Ajax.post({u: $images.data('reload-url'), ep: {index: data.result.index}});
    }).on('fileuploadfail', function (e, data) { // выводим ошибку аякса
        $images.find('.progress').collapse('hide');
        console.log('fileuploadfail', data);
        addAlert($images, 'ERROR', '<strong>Ошибка!</strong> Загрузка не удалась');
    });
}

$('#btn-image-add').fileupload({
    url: $images.data('upload-url'),
    headers: {'Wicket-Ajax': true, 'Wicket-Ajax-BaseURL': Wicket.Ajax.baseUrl, 'Accept': 'application/json'},
    dataType: 'json',
    acceptFileTypes: /(\.|\/)(jpe?g|png)$/i,
    formData: null,
    previewMaxHeight: 180,
    previewMaxWidth: 280,
    imageQuality: 100,
    dropZone: $imagesSelect,
    add: function (e, data) { // при добавлении файла сразу создаем елемент в #imageselect
        console.log('fileuploadadd', data);
        if (data.files.length == 1) {
            var file = data.files[0];
            Wicket.Ajax.post({
                u: $images.data('add-url'),
                coh: [function () {
                    file.$selectorItem = $images.find('.list-group.select .active');
                    file.ctype = 'main';
                    initFileData(file);
                    data.formData = {ctype: file.ctype, index: file.index};
                    console.log('Wicket.Ajax.post add-url coh', data);
                    data.submit(); // начинаем загрузку
                }]
            });
            $images.find('.progress').collapse('show'); // показываем прогресбар
        }
    }
});

configFileUpload('#btn-image-add');
$('.image-data-color, .image-data-main').each(function () {
    initFormItemFileUpload(this);
});

$(function () {
    $('.issue-date-input').datetimepicker({
        format: 'DD.MM.YYYY',
        locale: 'ru',
        icons: {
            time: 'fa fa-clock-o',
            date: 'fa fa-calendar',
            up: 'fa fa-chevron-up',
            down: 'fa fa-chevron-down',
            previous: 'fa fa-chevron-left',
            next: 'fa fa-chevron-right',
            today: 'fa fa-calendar-check-o',
            clear: 'fa fa-trash-o',
            close: 'fa fa-close'
        }
    });
});

var members = new Bloodhound({
    datumTokenizer: Bloodhound.tokenizers.obj.nonword('nickname'),
    queryTokenizer: Bloodhound.tokenizers.whitespace,
    prefetch: '/api/members/team/1?active=true',
    remote: {
        url: '/api/members/search?q=%QUERY',
        wildcard: '%QUERY'
    }
});


function initMemberTypehead() {
    $(this).typeahead({
        hint: true,
        highlight: true,
        minLength: 0
    }, {
        name: 'members',
        display: 'nickname',
        source: members,
        limit: 50
    });
}

$('#staff').on('addnewitem', function (e, d) {
    $(d.form).find('.member-input.typeahead').each(initMemberTypehead);
});
$('.member-input.typeahead').each(initMemberTypehead);

