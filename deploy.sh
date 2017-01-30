echo "Deleting previous function"
aws lambda delete-function \
    --region eu-central-1 \
    --function-name ricardovz_uploader \

echo "Creating function"
aws lambda create-function \
    --region eu-central-1 \
    --function-name ricardovz_uploader \
    --zip-file fileb://./build/distributions/ricardovz.com_uploader-1.0.zip \
    --role arn:aws:iam::430132907316:role/initiativeRole \
    --handler org.generationinitiative.uploader.Uploader::handleRequest \
    --runtime java8 \
    --timeout 10 \
    --environment Variables={JWT_CLIENT_SECRET=To_Be_Replaced}

echo "Creating alias"
aws lambda create-alias \
    --function-name ricardovz_uploader \
    --name ricardovz_uploader_alias \
    --region eu-central-1 \
    --function-version \$LATEST

echo "DONE"
